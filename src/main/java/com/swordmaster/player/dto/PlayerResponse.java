package com.swordmaster.player.dto;

import com.swordmaster.player.entity.Player;
import com.swordmaster.player.entity.PlayerArtifact;

import java.util.List;

public record PlayerResponse(
        Long                         playerId,
        int                          swordLevel,
        List<PlayerArtifactResponse> artifacts
) {
    public static PlayerResponse of(Player player, List<PlayerArtifact> artifacts) {
        return new PlayerResponse(
                player.getId(),
                player.getSwordLevel(),
                artifacts.stream().map(PlayerArtifactResponse::from).toList()
        );
    }
}
