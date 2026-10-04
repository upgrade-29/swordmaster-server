package com.swordmaster.equipment.sword;

import com.swordmaster.equipment.Equipment;
import com.swordmaster.equipment.EquipmentType;

public record Sword(
        int    level,               // PK
        String name,                // Unique
        Double nextSuccessRate,     // Null 허용
        Long   nextEnhanceCost,     // Null 허용
        long   sellPrice,
        long   attackPower,
        double attackSpeed,
        long   maxHp
) implements Equipment {
    @Override
    public EquipmentType type() { return EquipmentType.SWORD; }
}
