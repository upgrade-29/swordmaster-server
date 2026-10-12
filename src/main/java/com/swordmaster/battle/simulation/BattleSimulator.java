package com.swordmaster.battle.simulation;

import com.swordmaster.battle.dto.CombatStat;

// 세부 스테이지 전투 시뮬레이션 (클라 BattleEngine.cs + BattleSubStage.cs의 RunToEnd와 같은 결과를 내야 함)
// 계산 순서나 식을 바꾸면 클라와 double 값이 달라지므로 클라 코드와 함께 수정해야 함
public class BattleSimulator {
    // 부동소수점 오차 때문에 같은 시각의 공격이 미세하게 어긋나는 것을 같은 시각으로 봄
    private static final double TIME_EPSILON = 1e-9;

    private final double critMultiplier;    // config CRIT_MULTIPLIER
    private final double healRate;          // config HEAL_RATE (처치 시 최대 체력 대비 회복 비율)

    public BattleSimulator(double critMultiplier, double healRate) {
        this.critMultiplier = critMultiplier;
        this.healRate       = healRate;
    }

    // 세부 스테이지 하나를 끝까지 진행
    public SubStageResult simulate(
            CombatStat player,
            double     playerStartHp,
            CombatStat enemy,
            double     timeLimit,           // 일반 NORMAL_BATTLE_TIME_LIMIT, 보스 BOSS_BATTLE_TIME_LIMIT
            long       battleSeed,
            int        subStageIndex) {
        SplitMix64 random = SplitMix64.forSubStage(battleSeed, subStageIndex);

        double playerHp          = playerStartHp;
        double enemyHp           = enemy.maxHp();
        int    playerAttackCount = 0;
        int    enemyAttackCount  = 0;

        while (true) {
            // 같은 시각이면 플레이어가 먼저 공격
            double  nextPlayerTime = getAttackTime(player, playerAttackCount + 1);
            double  nextEnemyTime  = getAttackTime(enemy,  enemyAttackCount  + 1);
            boolean isPlayerTurn   = nextPlayerTime <= nextEnemyTime + TIME_EPSILON;
            double  time           = isPlayerTurn ? nextPlayerTime : nextEnemyTime;

            // 제한 시간 안에 더 이상 공격이 없으면 시간 초과
            if (time > timeLimit + TIME_EPSILON) {
                return new SubStageResult(
                        BattleSubStageResultEnum.TIME_OVER, playerHp, enemyHp, timeLimit,
                        playerAttackCount, enemyAttackCount, 0
                );
            }

            if (isPlayerTurn) {
                playerAttackCount++;

                double damage = getDamage(player, random);

                playerHp += getLifesteal(player, playerHp, damage);
                enemyHp   = Math.max(0, enemyHp - damage);
            } else {
                enemyAttackCount++;

                double damage = getDamage(enemy, random);

                enemyHp  += getLifesteal(enemy, enemyHp, damage);
                playerHp  = Math.max(0, playerHp - damage);
            }

            if (enemyHp <= 0) {
                return new SubStageResult(
                        BattleSubStageResultEnum.ENEMY_DEAD, playerHp, enemyHp, time,
                        playerAttackCount, enemyAttackCount, getHealOnKill(player, playerHp)
                );
            }

            if (playerHp <= 0) {
                return new SubStageResult(
                        BattleSubStageResultEnum.PLAYER_DEAD, playerHp, enemyHp, time,
                        playerAttackCount, enemyAttackCount, 0
                );
            }
        }
    }

    // n번째 공격 시각 = n x (1 / 공격속도), 누적 덧셈이 아니므로 오차가 쌓이지 않음
    private static double getAttackTime(CombatStat attacker, int attackNumber) {
        return attackNumber * (1 / attacker.attackSpeed());
    }

    // 공격 1회마다 치명타 판정에 난수 1개 사용
    private double getDamage(CombatStat attacker, SplitMix64 random) {
        boolean isCrit = random.nextDouble() < attacker.critRate();

        return attacker.attack() * (isCrit ? critMultiplier : 1);
    }

    // 흡혈량은 치명타를 포함한 피해량 기준, 최대 체력을 넘지 않음
    private static double getLifesteal(CombatStat attacker, double attackerHp, double damage) {
        return Math.min(damage * attacker.lifesteal(), attacker.maxHp() - attackerHp);
    }

    // 처치 시 최대 체력의 일정 비율 회복, 최대 체력을 넘지 않음
    private double getHealOnKill(CombatStat player, double playerHp) {
        return Math.min(player.maxHp() * healRate, player.maxHp() - playerHp);
    }

    // 적 스탯 (클라 StatCalculator.CalculateEnemy, 스테이지 배율은 공격력과 최대 체력에만 적용)
    public static CombatStat enemyStat(CombatStat base, double statMultiplier) {
        return new CombatStat(
                base.attack() * statMultiplier,
                base.attackSpeed(),
                Math.floor(base.maxHp() * statMultiplier),
                base.critRate(),
                base.lifesteal()
        );
    }
}
