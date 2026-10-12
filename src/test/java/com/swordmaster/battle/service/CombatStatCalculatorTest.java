package com.swordmaster.battle.service;

import com.swordmaster.battle.dto.CombatStat;
import com.swordmaster.equipment.EquipmentGrade;
import com.swordmaster.equipment.EquipmentStat;
import com.swordmaster.equipment.artifact.table.Artifact;
import com.swordmaster.equipment.artifact.table.ArtifactTable;
import com.swordmaster.equipment.sword.table.Sword;
import com.swordmaster.equipment.sword.table.SwordTable;
import com.swordmaster.player.entity.Player;
import com.swordmaster.player.entity.PlayerArtifact;
import com.swordmaster.user.entity.User;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CombatStatCalculatorTest {
    private static final Sword SWORD = new Sword(3, "검", 0.5, 100L, 0, 245, 1.2, 3200, "SWORD_03");

    private static PlayerArtifact artifact(String code, int level, Integer slot) {
        PlayerArtifact artifact = mock(PlayerArtifact.class);

        when(artifact.getCode()).thenReturn(code);
        lenient().when(artifact.getLevel()).thenReturn(level);
        lenient().when(artifact.getEquipSlot()).thenReturn(slot);

        return artifact;
    }

    private static CombatStatCalculator calculator() {
        SwordTable    swordTable    = mock(SwordTable.class);
        ArtifactTable artifactTable = mock(ArtifactTable.class);

        when(swordTable.findByLevel(3)).thenReturn(Optional.of(SWORD));
        when(artifactTable.getAll()).thenReturn(List.of(
                new Artifact(EquipmentGrade.COMMON, EquipmentStat.CRIT_RATE,    0.1,  0.02, "치명", "CRIT_01"),
                new Artifact(EquipmentGrade.RARE,   EquipmentStat.ATTACK_SPEED, 0.15, 0.05, "공속", "SPD_01"),
                new Artifact(EquipmentGrade.EPIC,   EquipmentStat.MAX_HP,       100.5, 10,  "체력", "HP_01"),
                new Artifact(EquipmentGrade.EPIC,   EquipmentStat.LIFESTEAL,    0.05, 0.01, "흡혈", "LIFE_01")
        ));

        return new CombatStatCalculator(swordTable, artifactTable);
    }

    @Test
    void swordOnly() {
        CombatStat stat = calculator().calculate(new Player(mock(User.class), 3), List.of());

        assertEquals(new CombatStat(245, 1.2, 3200, 0, 0), stat);
    }

    // 장착한 아티팩트만 더함, 레벨 0이면 baseValue, 최대 체력은 내림
    @Test
    void withEquippedArtifacts() {
        List<PlayerArtifact> artifacts = List.of(
                artifact("CRIT_01", 0, 0),      // 0.1
                artifact("SPD_01",  2, 1),      // 0.15 + 0.05 * 2 = 0.25
                artifact("HP_01",   1, 2),      // 100.5 + 10 = 110.5
                artifact("LIFE_01", 5, null)    // 미장착
        );

        CombatStat stat = calculator().calculate(new Player(mock(User.class), 3), artifacts);

        assertEquals(245,          stat.attack());
        assertEquals(1.2 + 0.25,   stat.attackSpeed());
        assertEquals(3310,         stat.maxHp());          // floor(3200 + 110.5)
        assertEquals(0.1,          stat.critRate());
        assertEquals(0,            stat.lifesteal());
    }

    @Test
    void unknownArtifact_fails() {
        List<PlayerArtifact> artifacts = List.of(artifact("NONE", 0, 0));

        assertThrows(IllegalStateException.class,
                () -> calculator().calculate(new Player(mock(User.class), 3), artifacts));
    }
}
