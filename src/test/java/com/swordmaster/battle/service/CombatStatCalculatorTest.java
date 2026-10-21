package com.swordmaster.battle.service;

import com.swordmaster.battle.dto.CombatStat;
import com.swordmaster.equipment.EquipmentStat;
import com.swordmaster.equipment.artifact.dto.ArtifactResponse;
import com.swordmaster.equipment.artifact.table.ArtifactStat;
import com.swordmaster.equipment.artifact.table.ArtifactStatTable;
import com.swordmaster.equipment.sword.table.Sword;
import com.swordmaster.equipment.sword.table.SwordTable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CombatStatCalculatorTest {
    private static final Sword SWORD = new Sword(3, "검", 0.5, 100L, 0, 245, 1.2, 3200, "SWORD_03");

    private static ArtifactResponse artifact(String code, int level, Integer slot) {
        return new ArtifactResponse(code, level, 0, slot);
    }

    private static CombatStatCalculator calculator() {
        SwordTable        swordTable        = mock(SwordTable.class);
        ArtifactStatTable artifactStatTable = mock(ArtifactStatTable.class);    // 등록하지 않은 code는 빈 리스트

        when(swordTable.findByLevel(3)).thenReturn(Optional.of(SWORD));
        when(artifactStatTable.findByCode("CRIT_01")).thenReturn(List.of(
                new ArtifactStat("CRIT_01", EquipmentStat.CRIT_RATE,    0.1,   0.02)));
        when(artifactStatTable.findByCode("SPD_01")).thenReturn(List.of(
                new ArtifactStat("SPD_01",  EquipmentStat.ATTACK_SPEED, 0.15,  0.05)));
        when(artifactStatTable.findByCode("HP_01")).thenReturn(List.of(
                new ArtifactStat("HP_01",   EquipmentStat.MAX_HP,       100.5, 10)));
        when(artifactStatTable.findByCode("LIFE_01")).thenReturn(List.of(
                new ArtifactStat("LIFE_01", EquipmentStat.LIFESTEAL,    0.05,  0.01)));
        when(artifactStatTable.findByCode("MULTI_01")).thenReturn(List.of(        // 스텟 여러 개
                new ArtifactStat("MULTI_01", EquipmentStat.MAX_HP,      50,    5),
                new ArtifactStat("MULTI_01", EquipmentStat.CRIT_RATE,   0.03,  0.01),
                new ArtifactStat("MULTI_01", EquipmentStat.LIFESTEAL,   0.02,  0.005)));

        return new CombatStatCalculator(swordTable, artifactStatTable);
    }

    @Test
    void swordOnly() {
        CombatStat stat = calculator().calculate(3, List.of());

        assertEquals(new CombatStat(245, 1.2, 3200, 0, 0), stat);
    }

    // 장착한 아티팩트만 더함, 레벨 1이면 baseValue, 최대 체력은 내림
    @Test
    void withEquippedArtifacts() {
        List<ArtifactResponse> artifacts = List.of(
                artifact("CRIT_01", 1, 0),      // 0.1
                artifact("SPD_01",  3, 1),      // 0.15 + 0.05 * 2 = 0.25
                artifact("HP_01",   2, 2),      // 100.5 + 10 = 110.5
                artifact("LIFE_01", 5, null)    // 미장착
        );

        CombatStat stat = calculator().calculate(3, artifacts);

        assertEquals(245,          stat.attack());
        assertEquals(1.2 * (1 + 0.25), stat.attackSpeed()); // 검 공격 속도에 대한 비율
        assertEquals(3310,         stat.maxHp());          // floor(3200 + 110.5)
        assertEquals(0.1,          stat.critRate());
        assertEquals(0,            stat.lifesteal());
    }

    // 아티팩트 하나의 스텟을 모두 더함
    @Test
    void withMultiStatArtifact() {
        List<ArtifactResponse> artifacts = List.of(
                artifact("MULTI_01", 3, 0)      // 체력 50 + 5 * 2 = 60, 치명 0.03 + 0.01 * 2, 흡혈 0.02 + 0.005 * 2
        );

        CombatStat stat = calculator().calculate(3, artifacts);

        assertEquals(245,                 stat.attack());
        assertEquals(1.2,                 stat.attackSpeed());
        assertEquals(3260,                stat.maxHp());
        assertEquals(0.03 + 0.01 * 2,     stat.critRate());
        assertEquals(0.02 + 0.005 * 2,    stat.lifesteal());
    }

    @Test
    void unknownArtifact_fails() {
        List<ArtifactResponse> artifacts = List.of(artifact("NONE", 1, 0));

        assertThrows(IllegalStateException.class,
                () -> calculator().calculate(3, artifacts));
    }
}
