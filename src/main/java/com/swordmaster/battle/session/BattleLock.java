package com.swordmaster.battle.session;

import com.swordmaster.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

// 유저 단위 전투 요청 락 (battle:lock:{userId})
// 같은 유저의 시작/보고/포기 요청이 동시에 처리되지 않도록 함
@Component
public class BattleLock {
    private static final String KEY = "battle:lock:";

    // 내가 잡은 락일 때만 해제 (TTL 만료 후 다른 요청이 잡은 락을 지우지 않도록)
    private static final RedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redis;
    private final Duration            ttl;

    public BattleLock(StringRedisTemplate redis, @Value("${battle.lock-ttl:5s}") Duration ttl) {
        this.redis = redis;
        this.ttl   = ttl;
    }

    // 락을 잡은 상태로 실행 (이미 처리 중인 요청이 있으면 즉시 실패)
    public <T> T execute(Long userId, Supplier<T> action) {
        String  key    = KEY + userId;
        String  token  = UUID.randomUUID().toString();
        Boolean locked = redis.opsForValue().setIfAbsent(key, token, ttl);

        if (!Boolean.TRUE.equals(locked)) {
            throw new BusinessException(HttpStatus.CONFLICT, "이전 전투 요청을 처리 중입니다.");
        }

        try {
            return action.get();
        } finally {
            redis.execute(UNLOCK_SCRIPT, List.of(key), token);
        }
    }
}
