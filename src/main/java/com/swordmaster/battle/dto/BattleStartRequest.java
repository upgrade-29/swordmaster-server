package com.swordmaster.battle.dto;

import jakarta.validation.constraints.Min;

public record BattleStartRequest(
        @Min(value = 1, message = "stage는 1 이상이어야 합니다.")
        int stage
) {
}
