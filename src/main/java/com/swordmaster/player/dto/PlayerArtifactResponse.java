package com.swordmaster.player.dto;

import com.swordmaster.player.entity.PlayerArtifact;

public record PlayerArtifactResponse(
        Long    id,         // 인벤토리에 중복되는 Artifact를 구분
        String  code,       // 아이템 종류 구분
        int     level,
        Integer equipSlot   // null 이면 미장착
) {
    public static PlayerArtifactResponse from(PlayerArtifact artifact) {
        return new PlayerArtifactResponse(
                artifact.getId(),
                artifact.getCode(),
                artifact.getLevel(),
                artifact.getEquipSlot()
        );
    }
}
