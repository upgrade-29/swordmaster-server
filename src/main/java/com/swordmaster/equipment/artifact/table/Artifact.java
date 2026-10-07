package com.swordmaster.equipment.artifact.table;

import com.swordmaster.equipment.EquipmentGrade;
import com.swordmaster.equipment.EquipmentStat;

public record Artifact(
        EquipmentGrade grade,
        EquipmentStat  statType,
        double         baseValue,
        double         valuePerLevel,
        String         name,
        String         code             // PK (?)
) {
}
