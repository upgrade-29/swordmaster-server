package com.swordmaster.battle.dto;

import java.time.Instant;

public record StageProgress(
        int     clearedStage,               // 도전 가능 스테이지는 1 ~ clearedStage + 1
        Instant nextBattleAvailableAt
) {
}
