package com.swordmaster.player.dto;

import com.swordmaster.equipment.EquipmentRarity;
import com.swordmaster.equipment.EquipmentStat;
import com.swordmaster.player.entity.PlayerArtifact;

public record PlayerArtifactResponse(
        Long            id,         // 인벤토리에 중복되는 Artifact를 구분
        EquipmentStat   stat,
        EquipmentRarity rarity,
        int             level,
        Integer         equipSlot   // null 이면 미장착
) {
    public static PlayerArtifactResponse from(PlayerArtifact artifact) {
        return new PlayerArtifactResponse(
                artifact.getId(),
                artifact.getStat(),
                artifact.getRarity(),
                artifact.getLevel(),
                artifact.getEquipSlot()
        );
    }
}
