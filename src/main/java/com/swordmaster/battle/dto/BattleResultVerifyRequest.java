package com.swordmaster.battle.dto;

import com.swordmaster.battle.simulation.BattleSubStageResultEnum;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// 세부 스테이지 종료 시 클라이언트가 보내는 전투 요약
public record BattleResultVerifyRequest(
        @NotBlank(message = "battleId가 비어 있습니다.")
        String battleId,

        @NotBlank(message = "gameDataVersion이 비어 있습니다.")
        String gameDataVersion,

        @Min(value = 0, message = "subStageIndex는 0 이상이어야 합니다.")
        int subStageIndex,

        @NotNull(message = "resultEnum이 비어 있습니다.")
        BattleSubStageResultEnum resultEnum,

        double playerHp,        // 처치 회복을 더하기 전 값
        double enemyHp,         // 처치했으면 0
        double duration,        // 시뮬레이션 시간(초), 배속 무관

        @Min(value = 0, message = "playerAttackCount는 0 이상이어야 합니다.")
        int playerAttackCount,

        @Min(value = 0, message = "enemyAttackCount는 0 이상이어야 합니다.")
        int enemyAttackCount
) {
}
