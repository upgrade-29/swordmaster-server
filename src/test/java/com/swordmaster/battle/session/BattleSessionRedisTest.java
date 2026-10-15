package com.swordmaster.battle.session;

import com.swordmaster.battle.BattleEndReason;
import com.swordmaster.battle.BattleErrorCode;
import com.swordmaster.battle.dto.BattleResultVerifyResponse;
import com.swordmaster.battle.dto.CombatStat;
import com.swordmaster.battle.simulation.BattleSubStageResultEnum;
import com.swordmaster.common.BusinessException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

// 실제 Redis(localhost:6379)가 필요한 테스트 (연결할 수 없으면 건너뜀)
// 다른 데이터와 겹치지 않도록 임의의 userId / battleId를 사용하고, 사용한 키만 삭제
class BattleSessionRedisTest {
    private static LettuceConnectionFactory factory;
    private static StringRedisTemplate      redis;

    private static final CombatStat STAT = new CombatStat(245.0, 1.35, 3200.0, 0.18, 0.05);

    private final Long   userId   = ThreadLocalRandom.current().nextLong(1_000_000_000L, Long.MAX_VALUE);
    private final String battleId = UUID.randomUUID().toString().replace("-", "");

    private BattleSessionRepository repository;
    private BattleLock              lock;

    @BeforeAll
    static void connect() {
        factory = new LettuceConnectionFactory(new RedisStandaloneConfiguration("localhost", 6379));
        factory.afterPropertiesSet();
        factory.start();

        redis = new StringRedisTemplate(factory);

        boolean available;

        try {
            available = "PONG".equals(redis.execute(connection -> connection.ping(), true));
        } catch (Exception e) {
            available = false;
        }

        assumeTrue(available, "Redis(localhost:6379)에 연결할 수 없어 테스트를 건너뜁니다.");
    }

    @AfterAll
    static void disconnect() {
        if (factory != null) factory.destroy();
    }

    @AfterEach
    void cleanUp() {
        redis.delete(List.of(
                "battle:session:" + userId,
                "battle:end:"     + battleId,
                "battle:lock:"    + userId
        ));
    }

    private void init(Duration sessionTtl, Duration lockTtl) {
        repository = new BattleSessionRepository(redis, JsonMapper.builder().build(), sessionTtl, Duration.ofMinutes(10));
        lock       = new BattleLock(redis, lockTtl);
    }

    private BattleSession newSession() {
        return BattleSession.start(battleId, 12, 8412093746512038871L, STAT, "1.0.3", Instant.now());
    }

    @Test
    void session_saveFindDelete() {
        init(Duration.ofMinutes(30), Duration.ofSeconds(5));

        BattleSession session = newSession();

        assertTrue(repository.find(userId).isEmpty());
        assertTrue(repository.saveIfAbsent(userId, session));
        assertEquals(session, repository.find(userId).orElseThrow());

        repository.delete(userId);

        assertTrue(repository.find(userId).isEmpty());
    }

    @Test
    void saveIfAbsent_rejectsSecondBattle() {
        init(Duration.ofMinutes(30), Duration.ofSeconds(5));

        BattleSession first  = newSession();
        BattleSession second = BattleSession.start("other", 1, 1L, STAT, "1.0.3", Instant.now());

        assertTrue(repository.saveIfAbsent(userId, first));
        assertFalse(repository.saveIfAbsent(userId, second));
        assertEquals(first, repository.find(userId).orElseThrow());   // 기존 세션이 유지됨
    }

    @Test
    void save_updatesSessionAndRefreshesTtl() {
        init(Duration.ofMinutes(30), Duration.ofSeconds(5));

        repository.saveIfAbsent(userId, newSession());
        redis.expire("battle:session:" + userId, Duration.ofSeconds(10));    // 만료 직전 상태로 만듦

        BattleResultVerifyResponse response = new BattleResultVerifyResponse(
                0, BattleSubStageResultEnum.ENEMY_DEAD, 1, 3036.4, 120, null
        );
        BattleSession advanced = repository.find(userId).orElseThrow()
                .advance(3036.4, 120, Instant.now(), response);

        repository.save(userId, advanced);

        assertEquals(advanced, repository.find(userId).orElseThrow());
        assertTrue(redis.getExpire("battle:session:" + userId) > 60, "TTL이 다시 설정되어야 합니다.");
    }

    @Test
    void session_expiresAfterTtl() throws InterruptedException {
        init(Duration.ofSeconds(1), Duration.ofSeconds(5));

        repository.saveIfAbsent(userId, newSession());
        Thread.sleep(1_500);

        assertTrue(repository.find(userId).isEmpty());
    }

    @Test
    void find_unreadableValue_returnsEmpty() {
        init(Duration.ofMinutes(30), Duration.ofSeconds(5));

        redis.opsForValue().set("battle:session:" + userId, "{\"unknown\":true}");

        assertTrue(repository.find(userId).isEmpty());
    }

    @Test
    void endResult_saveFind() {
        init(Duration.ofMinutes(30), Duration.ofSeconds(5));

        BattleResultVerifyResponse response = new BattleResultVerifyResponse(
                3, BattleSubStageResultEnum.PLAYER_DEAD, 3, 0.0, 360, null
        );
        BattleEndResult result = new BattleEndResult(BattleEndReason.PLAYER_DEAD, 3, null, response);

        assertTrue(repository.findEndResult(battleId).isEmpty());

        repository.saveEndResult(battleId, result);

        assertEquals(result, repository.findEndResult(battleId).orElseThrow());
        assertTrue(redis.getExpire("battle:end:" + battleId) > 0);
    }

    @Test
    void lock_releasedAfterExecute_evenOnException() {
        init(Duration.ofMinutes(30), Duration.ofSeconds(5));

        assertEquals("ok", lock.execute(userId, () -> "ok"));
        assertFalse(redis.hasKey("battle:lock:" + userId));

        assertThrows(IllegalStateException.class, () -> lock.execute(userId, () -> {
            throw new IllegalStateException("fail");
        }));
        assertFalse(redis.hasKey("battle:lock:" + userId));
    }

    @Test
    void lock_rejectsNestedRequest() {
        init(Duration.ofMinutes(30), Duration.ofSeconds(5));

        BusinessException e = assertThrows(BusinessException.class,
                () -> lock.execute(userId, () -> lock.execute(userId, () -> "inner")));

        assertEquals(HttpStatus.CONFLICT, e.getStatus());
        assertEquals(BattleErrorCode.REQUEST_IN_PROGRESS.name(), e.getCode());
    }

    @Test
    void lock_doesNotDeleteOtherRequestsLock() {
        init(Duration.ofMinutes(30), Duration.ofMillis(300));

        lock.execute(userId, () -> {
            try {
                Thread.sleep(600);      // 내 락이 TTL로 만료됨
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            redis.opsForValue().set("battle:lock:" + userId, "other-request");   // 다른 요청이 락을 잡음

            return null;
        });

        assertEquals("other-request", redis.opsForValue().get("battle:lock:" + userId));
    }

    @Test
    void lock_concurrentRequests_onlyOneRuns() throws InterruptedException {
        init(Duration.ofMinutes(30), Duration.ofSeconds(5));

        int             threads  = 10;
        ExecutorService pool     = Executors.newFixedThreadPool(threads);
        CountDownLatch  ready    = new CountDownLatch(threads);
        CountDownLatch  go       = new CountDownLatch(1);
        CountDownLatch  done     = new CountDownLatch(threads);
        AtomicInteger   success  = new AtomicInteger();
        AtomicInteger   conflict = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                ready.countDown();

                try {
                    go.await();
                    lock.execute(userId, () -> {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }

                        return success.incrementAndGet();
                    });
                } catch (BusinessException e) {
                    conflict.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        go.countDown();
        done.await();
        pool.shutdown();

        assertEquals(1, success.get());
        assertEquals(threads - 1, conflict.get());
    }
}
