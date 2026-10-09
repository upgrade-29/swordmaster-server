package com.swordmaster.equipment.artifact.dto;

import com.swordmaster.equipment.artifact.entity.PlayerArtifact;

public record ArtifactResponse(
        String  code,       // 아이템 종류 구분
        int     level,
        int     materialCount,
        Integer equipSlot   // null 이면 미장착
) {
    public static ArtifactResponse from(PlayerArtifact artifact) {
        return new ArtifactResponse(
                artifact.getCode(),
                artifact.getLevel(),
                artifact.getMaterialCount(),
                artifact.getEquipSlot()
        );
    }
}
