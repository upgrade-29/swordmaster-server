package com.swordmaster.equipment.artifact;

import com.swordmaster.equipment.Equipment;
import com.swordmaster.equipment.EquipmentRarity;
import com.swordmaster.equipment.EquipmentStat;
import com.swordmaster.equipment.EquipmentType;

public record Artifact(
        EquipmentStat   stat,           // PK
        EquipmentRarity rarity,         // PK
        String          name,
        double          baseValue,
        double          valuePerLevel
) implements Equipment {
    @Override
    public EquipmentType type() { return EquipmentType.ARTIFACT; }
}
