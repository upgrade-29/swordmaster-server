package com.swordmaster.equipment.artifact.table;

import com.swordmaster.equipment.EquipmentStat;

public record ArtifactStat(
        String        code,         // FK, PK
        EquipmentStat statType,     // PK
        double        baseValue,
        double        valuePerLevel
) {
}
