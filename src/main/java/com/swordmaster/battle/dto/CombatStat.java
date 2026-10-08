package com.swordmaster.battle.dto;

// 플레이어 전투 스탯 (검 레벨 + 장착 아티팩트로 서버가 계산)
public record CombatStat(
        double attack,          // 1회 기본 피해량
        double attackSpeed,     // 초당 공격 횟수
        double maxHp,
        double critRate,        // 0 ~ 1 비율
        double lifesteal        // 0 ~ 1 비율 (최대 체력 초과 불가)
) {
}
