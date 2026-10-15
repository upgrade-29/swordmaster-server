package com.swordmaster.jwt.service;

import com.swordmaster.common.BusinessException;
import com.swordmaster.jwt.JwtProvider;
import com.swordmaster.user.dto.LoginResponse;
import com.swordmaster.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedisTokenService {

    private static final String PREFIX = "auth:session:";
    private static final long IDLE_MS = 30 * 60 * 1000L;
    private static final long MAX_LIFETIME_MS = 14 * 24 * 60 * 60 * 1000L;

    /*
     * KEYS[1]: 로그인 세션 키
     * ARGV: 작업, 사용자 ID, 현재 해시, 새 해시, 비활동 시간(ms), 최대 기간(ms)
     * 성공하면 사용자 ID 문자열, 실패하면 nil을 반환한다.
     */
    private static final DefaultRedisScript<String> SESSION_SCRIPT =
            new DefaultRedisScript<>("""
                    local key = KEYS[1]
                    local op = ARGV[1]

                    local time = redis.call('TIME')
                    local now =
                        tonumber(time[1]) * 1000
                        + math.floor(tonumber(time[2]) / 1000)

                    local idleMs = tonumber(ARGV[5])

                    if op == 'create' then
                        if redis.call('EXISTS', key) == 1 then
                            return nil
                        end

                        local maxMs = tonumber(ARGV[6])

                        redis.call(
                            'HSET', key,
                            'userId', ARGV[2],
                            'refreshHash', ARGV[3],
                            'absoluteEnd', tostring(now + maxMs)
                        )

                        redis.call(
                            'PEXPIRE', key,
                            math.min(idleMs, maxMs)
                        )

                        return ARGV[2]
                    end

                    local userId = redis.call('HGET', key, 'userId')
                    if not userId then
                        return nil
                    end

                    local absoluteEnd = tonumber(
                        redis.call('HGET', key, 'absoluteEnd')
                    )

                    local ttl = redis.call('PTTL', key)

                    if not absoluteEnd
                        or absoluteEnd <= now
                        or ttl <= 0 then
                        redis.call('DEL', key)
                        return nil
                    end

                    if op == 'check' or op == 'touch' then
                        if userId ~= ARGV[2] then
                            return nil
                        end

                        if op == 'touch' then
                            redis.call(
                                'PEXPIRE', key,
                                math.min(idleMs, absoluteEnd - now)
                            )
                        end

                        return userId
                    end

                    local currentHash =
                        redis.call('HGET', key, 'refreshHash')

                    if currentHash ~= ARGV[3] then
                        return nil
                    end

                    if op == 'refreshCheck' then
                        return userId
                    end

                    if op == 'rotate' then
                        redis.call(
                            'HSET', key,
                            'refreshHash', ARGV[4]
                        )

                        -- refresh 자체는 활동으로 계산하지 않는다.
                        -- HSET은 기존 키의 TTL을 유지한다.
                        return userId
                    end

                    if op == 'revoke' then
                        redis.call('DEL', key)
                        return userId
                    end

                    return nil
                    """, String.class);

    private final StringRedisTemplate redisTemplate;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final SecureRandom random = new SecureRandom();

    // 비밀번호 검증을 통과한 로그인 처리에서만 호출한다.
    public LoginResponse issue(Long userId) {
        String sessionId = UUID.randomUUID().toString();
        String refreshToken = generateRefreshToken(sessionId);
        String accessToken = jwtProvider.createToken(userId, sessionId);

        String created = execute(
                "create", sessionId, userId.toString(), hash(refreshToken), ""
        );
        if (created == null) {
            throw new IllegalStateException("로그인 세션을 생성하지 못했습니다.");
        }
        return new LoginResponse(accessToken, refreshToken);
    }

    public LoginResponse refresh(String rawToken) {
        String sessionId = extractSessionId(rawToken);
        String oldHash = hash(rawToken);
        String userIdText = execute("refreshCheck", sessionId, "", oldHash, "");
        if (userIdText == null) {
            throw unauthorized();
        }

        Long userId = Long.valueOf(userIdText);
        if (!userRepository.existsById(userId)) {
            throw unauthorized();
        }

        String newRefreshToken = generateRefreshToken(sessionId);
        String newAccessToken = jwtProvider.createToken(userId, sessionId);

        // 사전 확인 이후의 동시 요청도 Lua에서 기존 해시를 재검사한다.
        String rotated = execute(
                "rotate", sessionId, "", oldHash, hash(newRefreshToken)
        );
        if (rotated == null) {
            throw unauthorized();
        }
        return new LoginResponse(newAccessToken, newRefreshToken);
    }

    public void logout(String rawToken) {
        String sessionId = extractSessionId(rawToken);
        execute("revoke", sessionId, "", hash(rawToken), "");
    }

    // 상태 확인은 비활동 시간을 연장하지 않는다.
    public boolean isActive(Long userId, String sessionId) {
        return execute("check", sessionId, userId.toString(), "", "") != null;
    }

    public boolean touch(Long userId, String sessionId) {
        return execute("touch", sessionId, userId.toString(), "", "") != null;
    }

    private String execute(
            String operation, String sessionId, String userId,
            String currentHash, String newHash
    ) {
        return redisTemplate.execute(
                SESSION_SCRIPT,
                List.of(PREFIX + sessionId),
                operation, userId, currentHash, newHash,
                Long.toString(IDLE_MS), Long.toString(MAX_LIFETIME_MS)
        );
    }

    private String generateRefreshToken(String sessionId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return sessionId + "." + Base64.getUrlEncoder()
                .withoutPadding().encodeToString(bytes);
    }

    private String extractSessionId(String rawToken) {
        if (rawToken == null || rawToken.length() != 80) {
            throw unauthorized();
        }
        String[] parts = rawToken.split("\\.", -1);
        if (parts.length != 2 || !parts[1].matches("[A-Za-z0-9_-]{43}")) {
            throw unauthorized();
        }
        try {
            String normalized = UUID.fromString(parts[0]).toString();
            if (!normalized.equals(parts[0])) {
                throw unauthorized();
            }
            return normalized;
        } catch (IllegalArgumentException e) {
            throw unauthorized();
        }
    }

    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private BusinessException unauthorized() {
        return new BusinessException(
                HttpStatus.UNAUTHORIZED,
                "로그인이 만료되었습니다. 다시 로그인해주세요."
        );
    }
}
