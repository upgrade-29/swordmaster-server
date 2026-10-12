package com.swordmaster.battle.dto;

import com.swordmaster.battle.simulation.BattleSubStageResultEnum;

public record BattleResultVerifyResponse(
        int                      subStageIndex,
        BattleSubStageResultEnum resultEnum,          // 서버가 계산한 결과
        int                      nextSubStageIndex,   // 전투가 끝났으면 subStageIndex와 같음
        double                   nextPlayerHp,        // 끝난 체력 + 처치 회복 (전투 종료 시 회복 없음)
        long                     accumulatedGold,
        BattleEndResponse        battleEnd            // 진행 중이면 null
) {
}
