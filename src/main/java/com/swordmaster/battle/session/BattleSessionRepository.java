package com.swordmaster.battle.session;

import com.swordmaster.battle.dto.BattleResultVerifyResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Optional;

// 키 구조
//  - battle:session:{userId}  진행 중인 전투 (유저당 하나만 존재)
//  - battle:end:{battleId}    종료된 전투의 마지막 응답 (세션 삭제 후 같은 보고가 다시 와도 같은 응답을 반환)
@Slf4j
@Repository
public class BattleSessionRepository {
    private static final String SESSION_KEY = "battle:session:";
    private static final String END_KEY     = "battle:end:";

    private final StringRedisTemplate redis;
    private final JsonMapper          mapper;
    private final Duration            sessionTtl;
    private final Duration            endResultTtl;

    public BattleSessionRepository(
            StringRedisTemplate redis,
            JsonMapper          mapper,
            @Value("${battle.session-ttl:30m}")    Duration sessionTtl,
            @Value("${battle.end-result-ttl:10m}") Duration endResultTtl) {
        this.redis        = redis;
        this.mapper       = mapper;
        this.sessionTtl   = sessionTtl;
        this.endResultTtl = endResultTtl;
    }

    // 세션 (조회)
    public Optional<BattleSession> find(Long userId) {
        return read(redis.opsForValue().get(SESSION_KEY + userId), BattleSession.class);
    }

    // 세션 (생성, 이미 진행 중인 전투가 있으면 false)
    public boolean saveIfAbsent(Long userId, BattleSession session) {
        Boolean saved = redis.opsForValue().setIfAbsent(SESSION_KEY + userId, write(session), sessionTtl);

        return Boolean.TRUE.equals(saved);
    }

    // 세션 (갱신, TTL도 다시 설정)
    public void save(Long userId, BattleSession session) {
        redis.opsForValue().set(SESSION_KEY + userId, write(session), sessionTtl);
    }

    // 세션 (삭제)
    public void delete(Long userId) {
        redis.delete(SESSION_KEY + userId);
    }

    // 종료 응답 (저장)
    public void saveEndResult(String battleId, BattleResultVerifyResponse response) {
        redis.opsForValue().set(END_KEY + battleId, write(response), endResultTtl);
    }

    // 종료 응답 (조회)
    public Optional<BattleResultVerifyResponse> findEndResult(String battleId) {
        return read(redis.opsForValue().get(END_KEY + battleId), BattleResultVerifyResponse.class);
    }

    // 공통 (직렬화)
    private String write(Object value) {
        return mapper.writeValueAsString(value);
    }

    // 공통 (역직렬화)
    // 구조가 바뀌어 읽을 수 없는 값은 없는 것으로 처리 (배포 직후 이전 형식의 세션)
    private <T> Optional<T> read(String json, Class<T> type) {
        if (json == null) return Optional.empty();

        try {
            return Optional.of(mapper.readValue(json, type));
        } catch (JacksonException e) {
            log.error("Redis 값을 읽을 수 없습니다: type={}, message={}", type.getSimpleName(), e.getOriginalMessage());

            return Optional.empty();
        }
    }
}
