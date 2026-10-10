package com.swordmaster.player.dto;

import com.swordmaster.currency.CurrencyType;
import com.swordmaster.equipment.artifact.dto.ArtifactResponse;
import com.swordmaster.player.entity.Player;

import java.util.List;
import java.util.Map;

public record PlayerResponse(
        Long                    playerId,
        int                     swordLevel,
        Map<CurrencyType, Long> currencies,
        List<ArtifactResponse>  artifacts
) {
    public static PlayerResponse of(
            Player                  player,
            Map<CurrencyType, Long> currencies,
            List<ArtifactResponse>  artifacts) {
        return new PlayerResponse(
                player.getId(),
                player.getSwordLevel(),
                currencies,
                artifacts
        );
    }
}
