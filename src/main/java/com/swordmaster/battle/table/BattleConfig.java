package com.swordmaster.battle.table;

import com.swordmaster.battle.simulation.BattleSimulator;
import com.swordmaster.common.table.ConfigTable;
import lombok.Getter;
import org.springframework.stereotype.Component;

// 전투 계산에 쓰는 config 값 (서버 부팅 시 한 번만 읽고, 키가 없으면 부팅 실패)
// 클라 BattleDB는 키가 없으면 기본값을 쓰지만, 서버는 검증 기준이므로 반드시 시트에 있어야 함
@Getter
@Component
public class BattleConfig {
    private final double          critMultiplier;     // CRIT_MULTIPLIER
    private final double          healRate;           // HEAL_RATE
    private final double          normalTimeLimit;    // NORMAL_BATTLE_TIME_LIMIT (초)
    private final double          bossTimeLimit;      // BOSS_BATTLE_TIME_LIMIT (초)
    private final BattleSimulator simulator;

    public BattleConfig(ConfigTable configTable) {
        critMultiplier  = configTable.getDouble("CRIT_MULTIPLIER");
        healRate        = configTable.getDouble("HEAL_RATE");
        normalTimeLimit = configTable.getDouble("NORMAL_BATTLE_TIME_LIMIT");
        bossTimeLimit   = configTable.getDouble("BOSS_BATTLE_TIME_LIMIT");

        if (critMultiplier < 1)
            throw new IllegalStateException("CRIT_MULTIPLIER는 1 이상이어야 합니다: " + critMultiplier);
        if (healRate < 0 || healRate > 1)
            throw new IllegalStateException("HEAL_RATE는 0 이상 1 이하여야 합니다: " + healRate);
        if (normalTimeLimit <= 0 || bossTimeLimit <= 0)
            throw new IllegalStateException("전투 시간 제한은 0보다 커야 합니다.");

        simulator = new BattleSimulator(critMultiplier, healRate);
    }

    // 일반 적 / 보스의 제한 시간 (클라 BattleEngine.GetTimeLimit)
    public double getTimeLimit(Enemy enemy) {
        return enemy.boss() ? bossTimeLimit : normalTimeLimit;
    }
}
