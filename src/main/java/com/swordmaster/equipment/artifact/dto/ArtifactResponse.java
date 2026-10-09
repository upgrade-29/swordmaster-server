package com.swordmaster.equipment.artifact.dto;

import com.swordmaster.equipment.artifact.entity.PlayerArtifact;

public record ArtifactResponse(
        Long    id,         // 인벤토리에 중복되는 Artifact를 구분
        String  code,       // 아이템 종류 구분
        int     level,
        Integer equipSlot   // null 이면 미장착
) {
    public static ArtifactResponse from(PlayerArtifact artifact) {
        return new ArtifactResponse(
                artifact.getId(),
                artifact.getCode(),
                artifact.getLevel(),
                artifact.getEquipSlot()
        );
    }
}
