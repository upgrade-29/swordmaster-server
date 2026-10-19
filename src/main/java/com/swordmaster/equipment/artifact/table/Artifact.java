package com.swordmaster.equipment.artifact.table;

import com.swordmaster.equipment.EquipmentGrade;

public record Artifact(
        String         code,        // PK
        String         name,
        EquipmentGrade grade
) {
}
