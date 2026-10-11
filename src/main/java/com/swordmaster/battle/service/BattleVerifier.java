package com.swordmaster.battle.service;

import com.swordmaster.battle.dto.BattleResultVerifyRequest;
import com.swordmaster.battle.simulation.SubStageResult;

import java.time.Duration;
import java.util.Optional;

// 클라가 보낸 세부 스테이지 요약을 서버 시뮬레이션 결과와 비교 (클라 LocalBattleSessionService.Verify와 같은 기준)
public final class BattleVerifier {
    public static final double MAX_BATTLE_SPEED = 2;      // 배속 상한 (클라 BattleManager.MaxSpeed)
    public static final double TIME_TOLERANCE   = 0.3;    // 시간 검증 허용 오차(초), 통신 지연 / 프레임 오차
    public static final double VALUE_EPSILON    = 1e-6;   // 체력, 시간 비교 허용 오차

    private BattleVerifier() {
    }

    // 다르면 이유를, 같으면 empty 반환
    public static Optional<String> verify(
            BattleResultVerifyRequest request,
            int                       expectedSubStageIndex,
            SubStageResult            expected,
            Duration                  elapsed) {
        if (request.subStageIndex() != expectedSubStageIndex)
            return Optional.of("subStageIndex " + request.subStageIndex() + " != " + expectedSubStageIndex);

        if (request.resultEnum() != expected.resultEnum())
            return Optional.of("result " + request.resultEnum() + " != " + expected.resultEnum());

        if (request.playerAttackCount() != expected.playerAttackCount() || request.enemyAttackCount() != expected.enemyAttackCount())
            return Optional.of("attack count " + request.playerAttackCount() + "/" + request.enemyAttackCount()
                    + " != " + expected.playerAttackCount() + "/" + expected.enemyAttackCount());

        if (!isClose(request.duration(), expected.duration()))
            return Optional.of("duration " + request.duration() + " != " + expected.duration());

        if (!isClose(request.playerHp(), expected.playerHp()) || !isClose(request.enemyHp(), expected.enemyHp()))
            return Optional.of("hp " + request.playerHp() + "/" + request.enemyHp()
                    + " != " + expected.playerHp() + "/" + expected.enemyHp());

        // 최대 배속으로 진행해도 이 시간보다 빨리 끝날 수는 없음
        double minSeconds     = expected.duration() / MAX_BATTLE_SPEED - TIME_TOLERANCE;
        double elapsedSeconds = elapsed.toNanos() / 1e9;

        if (elapsedSeconds < minSeconds)
            return Optional.of("too fast %.3fs < %.3fs".formatted(elapsedSeconds, minSeconds));

        return Optional.empty();
    }

    private static boolean isClose(double a, double b) {
        return Math.abs(a - b) <= VALUE_EPSILON;
    }
}
