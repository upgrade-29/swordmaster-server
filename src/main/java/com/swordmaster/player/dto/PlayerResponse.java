package com.swordmaster.player.dto;

import com.swordmaster.battle.dto.StageProgress;
import com.swordmaster.currency.dto.CurrenciesResponse;
import com.swordmaster.equipment.artifact.dto.ArtifactResponse;
import com.swordmaster.equipment.sword.dto.SwordResponse;

import java.util.List;

public record PlayerResponse(
        Long                   userId,
        String                 nickname,
        SwordResponse          swordResponse,
        CurrenciesResponse     currenciesResponse,
        List<ArtifactResponse> artifactResponses,
        StageProgress          stageProgress        // 클리어한 스테이지 (BattleEndResponse.stageProgress와 같은 형식)
) {
    public static PlayerResponse of(
            Long                   userId,
            String                 nickname,
            SwordResponse          swordResponse,
            CurrenciesResponse     currenciesResponse,
            List<ArtifactResponse> artifactResponses,
            StageProgress          stageProgress) {
        return new PlayerResponse(
                userId,
                nickname,
                swordResponse,
                currenciesResponse,
                artifactResponses,
                stageProgress
        );
    }
}
