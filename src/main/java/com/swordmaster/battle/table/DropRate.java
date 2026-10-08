package com.swordmaster.battle.table;

import com.swordmaster.equipment.EquipmentGrade;

// 아티팩트가 드랍될 때 등급별 확률
public record DropRate(
        EquipmentGrade grade,   // PK
        double         rate     // 0 ~ 1, 전체 합은 1
) {
}
