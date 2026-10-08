package com.swordmaster.battle.dto;

import java.util.List;

public record BattleRewards(
        long         gold,              // 세션의 accumulatedGold 최종값
        List<String> artifactCodes      // 없으면 빈 리스트
) {
}
