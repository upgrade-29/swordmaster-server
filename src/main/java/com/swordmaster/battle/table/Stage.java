package com.swordmaster.battle.table;

public record Stage(
        int    stage,               // PK (1부터)
        String enemyCode,           // 일반 적 (EnemyTable, boss = false)
        int    enemyCount,          // 일반 적 수, 세부 스테이지 수는 enemyCount + 1 (마지막이 보스)
        String bossCode,            // 보스 (EnemyTable, boss = true)
        double statMultiplier,      // 적의 공격력, 최대 체력에 곱하는 배율
        long   goldPerEnemy,
        long   bossGold,
        double artifactDropRate,    // 승리 시 아티팩트 드랍 확률 (0 ~ 1)
        long   recommendedPower,
        String imageCode
) {
    // 세부 스테이지 (클라 BattleEngine과 같은 규칙, index는 0부터)
    public int subStageCount() { return enemyCount + 1; }

    public boolean isBossSubStage(int subStageIndex) { return subStageIndex == enemyCount; }

    public String enemyCodeAt(int subStageIndex) {
        return isBossSubStage(subStageIndex) ? bossCode : enemyCode;
    }

    public long killGoldAt(int subStageIndex) {
        return isBossSubStage(subStageIndex) ? bossGold : goldPerEnemy;
    }
}
