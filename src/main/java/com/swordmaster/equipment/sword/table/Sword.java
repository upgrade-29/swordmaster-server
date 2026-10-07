package com.swordmaster.equipment.sword.table;

public record Sword(
        int    level,               // PK
        String name,                // Unique
        Double successRate,     // Null 허용
        Long   enhanceCost,     // Null 허용
        long   sellPrice,
        long   attackPower,
        double attackSpeed,
        long   maxHp,
        String appearanceCode
) {
}
