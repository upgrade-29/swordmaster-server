package com.swordmaster.equipment;

public record Sword(
        int    level,               // PK
        String name,                // Unique
        Double nextSuccessRate,     // Null 허용
        Long   nextEnhanceCost,     // Null 허용
        long   sellPrice,
        long   attackPower,
        double attackSpeed,
        long   maxHp
) {
}
