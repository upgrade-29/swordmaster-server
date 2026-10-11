package com.swordmaster.equipment.artifact.dto;

import com.swordmaster.equipment.artifact.entity.PlayerArtifact;

public record ArtifactResponse(
        String  code,       // 아이템 종류 구분
        int     level,
        int     materialCount,
        Integer equipSlot   // null 이면 미장착
) {
    public static ArtifactResponse from(PlayerArtifact playerArtifact) {
        return new ArtifactResponse(
                playerArtifact.getCode(),
                playerArtifact.getLevel(),
                playerArtifact.getMaterialCount(),
                playerArtifact.getEquipSlot()
        );
    }
}
