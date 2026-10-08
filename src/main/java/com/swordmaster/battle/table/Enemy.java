package com.swordmaster.battle.table;

import com.swordmaster.battle.dto.CombatStat;

public record Enemy(
        String  appearanceCode,
        boolean boss,
        double  attack,
        double  attackSpeed,
        double  maxHp,
        double  critRate,       // 0 ~ 1 비율
        double  lifesteal,      // 0 ~ 1 비율
        String  name,
        String  code            // PK
) {
    // 스테이지 배율 적용 전 기본 스탯
    public CombatStat baseStat() {
        return new CombatStat(attack, attackSpeed, maxHp, critRate, lifesteal);
    }
}
