package com.swordmaster.player.dto;

import com.swordmaster.currency.CurrencyType;
import com.swordmaster.player.entity.Player;
import com.swordmaster.player.entity.PlayerArtifact;

import java.util.List;
import java.util.Map;

public record PlayerResponse(
        Long                         playerId,
        int                          swordLevel,
        Map<CurrencyType, Long>      currencies,
        List<PlayerArtifactResponse> artifacts
) {
    public static PlayerResponse of(
            Player                  player,
            Map<CurrencyType, Long> currencies,
            List<PlayerArtifact>    artifacts) {
        return new PlayerResponse(
                player.getId(),
                player.getSwordLevel(),
                currencies,
                artifacts.stream().map(PlayerArtifactResponse::from).toList()
        );
    }
}
