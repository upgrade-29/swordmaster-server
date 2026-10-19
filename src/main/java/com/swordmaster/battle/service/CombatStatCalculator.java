package com.swordmaster.battle.service;

import com.swordmaster.battle.dto.CombatStat;
import com.swordmaster.equipment.artifact.table.ArtifactStat;
import com.swordmaster.equipment.artifact.table.ArtifactStatTable;
import com.swordmaster.equipment.sword.table.Sword;
import com.swordmaster.equipment.sword.table.SwordTable;
import com.swordmaster.equipment.artifact.dto.ArtifactResponse;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

// 플레이어 전투 스탯 (클라 StatCalculator.CalculatePlayer)
// 검 능력치에 장착한 아티팩트의 능력치를 더함, 클라는 서버가 내려준 값을 그대로 쓰므로 서버 계산이 기준
@Component
public class CombatStatCalculator {
    private final SwordTable        swordTable;
    private final ArtifactStatTable artifactStatTable;

    public CombatStatCalculator(SwordTable swordTable, ArtifactStatTable artifactStatTable) {
        this.swordTable        = swordTable;
        this.artifactStatTable = artifactStatTable;
    }

    // artifacts는 플레이어의 인벤토리 전체 (장착한 것만 계산)
    public CombatStat calculate(int swordLevel, List<ArtifactResponse> artifacts) {
        Sword sword = swordTable.findByLevel(swordLevel)
                .orElseThrow(() -> new IllegalStateException("검의 정보가 없습니다: level " + swordLevel));

        double attack      = sword.attackPower();
        double attackSpeed = sword.attackSpeed();
        double maxHp       = sword.maxHp();
        double critRate    = 0;
        double lifesteal   = 0;

        // 슬롯 순서로 더함 (더하는 순서가 바뀌면 double 결과가 미세하게 달라질 수 있음)
        List<ArtifactResponse> equipped = artifacts.stream()
                .filter(artifact -> artifact.equipSlot() != null)
                .sorted(Comparator.comparing(ArtifactResponse::equipSlot))
                .toList();

        for (ArtifactResponse equippedArtifact : equipped) {
            // 아티팩트 하나가 여러 스텟을 가짐 (statType 오름차순), 스텟 테이블 검증상 모든 아티팩트는 1개 이상의 스텟을 가짐
            List<ArtifactStat> stats = artifactStatTable.findByCode(equippedArtifact.code());

            if (stats.isEmpty()) throw new IllegalStateException("아티팩트 정보가 없습니다: " + equippedArtifact.code());

            for (ArtifactStat stat : stats) {
                double value = getArtifactValue(stat, equippedArtifact.level());

                switch (stat.statType()) {
                    case ATTACK_SPEED -> attackSpeed += value;
                    case MAX_HP       -> maxHp       += value;
                    case CRIT_RATE    -> critRate    += value;
                    case LIFESTEAL    -> lifesteal   += value;
                }
            }
        }

        return new CombatStat(attack, attackSpeed, Math.floor(maxHp), critRate, lifesteal);
    }

    // PlayerArtifact 레벨은 1부터 (획득 시 1, 1레벨이 baseValue, 클라 StatCalculator와 같은 계산)
    public static double getArtifactValue(ArtifactStat stat, int level) {
        return stat.baseValue() + stat.valuePerLevel() * (level - 1);
    }
}
