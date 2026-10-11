package com.swordmaster.player.dto;

import com.swordmaster.currency.dto.CurrenciesResponse;
import com.swordmaster.equipment.artifact.dto.ArtifactResponse;
import com.swordmaster.equipment.sword.dto.SwordResponse;

import java.util.List;

public record PlayerResponse(
        Long                   userId,
        String                 nickname,
        SwordResponse          swordResponse,
        CurrenciesResponse     currenciesResponse,
        List<ArtifactResponse> artifactResponses
) {
    public static PlayerResponse of(
            Long                   userId,
            String                 nickname,
            SwordResponse          swordResponse,
            CurrenciesResponse     currenciesResponse,
            List<ArtifactResponse> artifactResponses) {
        return new PlayerResponse(
                userId,
                nickname,
                swordResponse,
                currenciesResponse,
                artifactResponses
        );
    }
}
