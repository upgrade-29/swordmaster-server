package com.swordmaster.battle.dto;

import jakarta.validation.constraints.NotBlank;

public record BattleAbandonRequest(
        @NotBlank(message = "battleId가 비어 있습니다.")
        String battleId
) {
}
