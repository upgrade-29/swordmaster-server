package com.swordmaster.equipment.artifact.table;

import com.swordmaster.equipment.EquipmentGrade;

public record ArtifactEnhance(
        EquipmentGrade grade,           // PK
        int            level,           // PK
        int            materialCount,
        long           gold
) {
}
