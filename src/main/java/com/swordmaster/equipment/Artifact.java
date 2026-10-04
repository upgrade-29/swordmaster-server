package com.swordmaster.equipment;

public record Artifact(
        EquipmentStat   stat,           // PK
        EquipmentRarity rarity,         // PK
        String          name,
        double          baseValue,
        double          valuePerLevel
) {
}