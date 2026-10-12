package com.swordmaster.battle.dto;

import java.time.Instant;

// 전투 시작 / 재접속 시 클라이언트에 내려주는 세션 정보
public record BattleSessionInfo(
        String     battleId,
        int        stage,               // 1부터
        long       battleSeed,          // 0 이상 (클라가 ulong)
        CombatStat playerStat,
        int        subStageIndex,       // 0부터, 새 전투면 0
        double     playerHp,            // 세부 스테이지 시작 체력, 새 전투면 maxHp
        long       accumulatedGold,
        Instant    serverTime
) {
}
