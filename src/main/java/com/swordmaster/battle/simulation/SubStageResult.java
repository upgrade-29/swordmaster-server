package com.swordmaster.battle.simulation;

// 세부 스테이지 한 판의 시뮬레이션 결과 (클라의 BattleResultVerifyRequest와 비교하는 값)
public record SubStageResult(
        BattleSubStageResultEnum resultEnum,
        double                   playerHp,          // 처치 회복을 더하기 전
        double                   enemyHp,
        double                   duration,          // 처치/사망이면 마지막 공격 시각, 시간 초과면 제한 시간
        int                      playerAttackCount,
        int                      enemyAttackCount,
        double                   healOnKill         // 처치하지 못했으면 0
) {
}
