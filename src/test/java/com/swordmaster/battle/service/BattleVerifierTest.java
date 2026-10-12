package com.swordmaster.battle.service;

import com.swordmaster.battle.dto.BattleResultVerifyRequest;
import com.swordmaster.battle.simulation.BattleSubStageResultEnum;
import com.swordmaster.battle.simulation.SubStageResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class BattleVerifierTest {
    private static final SubStageResult EXPECTED = new SubStageResult(
            BattleSubStageResultEnum.ENEMY_DEAD, 2876.4, 0, 8.0, 10, 6, 320
    );

    private static BattleResultVerifyRequest request(int index, BattleSubStageResultEnum result, double playerHp,
                                                     double enemyHp, double duration, int playerCount, int enemyCount) {
        return new BattleResultVerifyRequest("b", index, result, playerHp, enemyHp, duration, playerCount, enemyCount);
    }

    private static BattleResultVerifyRequest valid() {
        return request(0, BattleSubStageResultEnum.ENEMY_DEAD, 2876.4, 0, 8.0, 10, 6);
    }

    @Test
    void pass() {
        assertTrue(BattleVerifier.verify(valid(), 0, EXPECTED, Duration.ofSeconds(8)).isEmpty());
    }

    @Test
    void pass_withinEpsilon() {
        BattleResultVerifyRequest request = request(0, BattleSubStageResultEnum.ENEMY_DEAD, 2876.4 + 5e-7, 0, 8.0 - 5e-7, 10, 6);

        assertTrue(BattleVerifier.verify(request, 0, EXPECTED, Duration.ofSeconds(8)).isEmpty());
    }

    @Test
    void fail_eachField() {
        Duration elapsed = Duration.ofSeconds(8);

        assertTrue(BattleVerifier.verify(valid(), 1, EXPECTED, elapsed).orElseThrow().startsWith("subStageIndex"));
        assertTrue(BattleVerifier.verify(request(0, BattleSubStageResultEnum.PLAYER_DEAD, 2876.4, 0, 8.0, 10, 6), 0, EXPECTED, elapsed)
                .orElseThrow().startsWith("result"));
        assertTrue(BattleVerifier.verify(request(0, BattleSubStageResultEnum.ENEMY_DEAD, 2876.4, 0, 8.0, 11, 6), 0, EXPECTED, elapsed)
                .orElseThrow().startsWith("attack count"));
        assertTrue(BattleVerifier.verify(request(0, BattleSubStageResultEnum.ENEMY_DEAD, 2876.4, 0, 8.1, 10, 6), 0, EXPECTED, elapsed)
                .orElseThrow().startsWith("duration"));
        assertTrue(BattleVerifier.verify(request(0, BattleSubStageResultEnum.ENEMY_DEAD, 2900, 0, 8.0, 10, 6), 0, EXPECTED, elapsed)
                .orElseThrow().startsWith("hp"));
    }

    // 전투 8초 / 최대 2배속 = 4초, 허용 오차 0.3초 -> 3.7초보다 빠르면 실패
    @Test
    void tooFast() {
        assertTrue(BattleVerifier.verify(valid(), 0, EXPECTED, Duration.ofMillis(3_701)).isEmpty());
        assertTrue(BattleVerifier.verify(valid(), 0, EXPECTED, Duration.ofMillis(3_699)).orElseThrow().startsWith("too fast"));
    }
}
