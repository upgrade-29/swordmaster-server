package com.swordmaster.battle.service;

import com.swordmaster.battle.dto.CombatStat;
import com.swordmaster.equipment.artifact.table.Artifact;
import com.swordmaster.equipment.artifact.table.ArtifactTable;
import com.swordmaster.equipment.sword.table.Sword;
import com.swordmaster.equipment.sword.table.SwordTable;
import com.swordmaster.equipment.artifact.dto.ArtifactResponse;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

// 플레이어 전투 스탯 (클라 StatCalculator.CalculatePlayer)
// 검 능력치에 장착한 아티팩트의 능력치를 더함, 클라는 서버가 내려준 값을 그대로 쓰므로 서버 계산이 기준
@Component
public class CombatStatCalculator {
    private final SwordTable            swordTable;
    private final Map<String, Artifact> artifactsByCode;

    public CombatStatCalculator(SwordTable swordTable, ArtifactTable artifactTable) {
        this.swordTable = swordTable;

        artifactsByCode = artifactTable.getAll().stream()
                .collect(Collectors.toUnmodifiableMap(Artifact::code, Function.identity()));
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
            Artifact artifact = artifactsByCode.get(equippedArtifact.code());

            if (artifact == null) throw new IllegalStateException("아티팩트 정보가 없습니다: " + equippedArtifact.code());

            double value = getArtifactValue(artifact, equippedArtifact.level());

            switch (artifact.statType()) {
                case ATTACK_SPEED -> attackSpeed += value;
                case MAX_HP       -> maxHp       += value;
                case CRIT_RATE    -> critRate    += value;
                case LIFESTEAL    -> lifesteal   += value;
            }
        }

        return new CombatStat(attack, attackSpeed, Math.floor(maxHp), critRate, lifesteal);
    }

    // PlayerArtifact 레벨은 0부터 (획득 시 0, 클라 StatCalculator의 level - 1과 같은 값)
    public static double getArtifactValue(Artifact artifact, int level) {
        return artifact.baseValue() + artifact.valuePerLevel() * level;
    }
}
