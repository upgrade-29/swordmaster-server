package com.swordmaster.player.dto;

import com.swordmaster.currency.dto.CurrenciesResponse;
import com.swordmaster.equipment.artifact.dto.ArtifactResponse;
import com.swordmaster.equipment.sword.dto.SwordResponse;
import com.swordmaster.user.dto.UserResponse;

import java.util.List;

public record PlayerResponse(
        Long                   userId,
        String                 nickname,
        SwordResponse          swordResponse,
        CurrenciesResponse     currenciesResponse,
        List<ArtifactResponse> artifactResponses
) {
    public static PlayerResponse of(
            UserResponse           user,
            SwordResponse          swordResponse,
            CurrenciesResponse     currenciesResponse,
            List<ArtifactResponse> artifactResponses) {
        return new PlayerResponse(
                user.id(),
                user.nickname(),
                swordResponse,
                currenciesResponse,
                artifactResponses
        );
    }
}
