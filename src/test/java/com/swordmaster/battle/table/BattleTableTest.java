package com.swordmaster.battle.table;

import com.swordmaster.common.table.GoogleSheetClient;
import com.swordmaster.common.table.constant.ConstantKey;
import com.swordmaster.common.table.constant.ConstantTable;
import com.swordmaster.equipment.EquipmentGrade;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// 시트 대신 고정된 행을 돌려주는 GoogleSheetClient로 테이블 변환 / 검증을 확인
// (GoogleSheetClient는 숫자를 Long / Double, 나머지는 String으로 넘겨주므로 같은 형태로 입력)
class BattleTableTest {
    private static final long ENEMY = 1, STAGE = 2, DROP = 3;

    private static GoogleSheetClient sheets(Map<Long, List<Map<String, Object>>> rowsByGid) {
        return new GoogleSheetClient("test") {
            @Override
            public List<Map<String, Object>> readSheet(long gid) { return rowsByGid.get(gid); }
        };
    }

    private static Map<String, Object> row(Object... keyValues) {
        Map<String, Object> row = new LinkedHashMap<>();

        for (int i = 0; i < keyValues.length; i += 2) row.put((String) keyValues[i], keyValues[i + 1]);

        return row;
    }

    private static Map<String, Object> enemy(String code, String boss) {
        return row("appearanceCode", "APP", "boss", boss, "attack", 8L, "attackSpeed", 0.8, "maxHp", 120L,
                   "critRate", 0L, "lifesteal", 0.05, "name", "적", "code", code);
    }

    private static Map<String, Object> stage(long stage, String enemyCode, String bossCode) {
        return row("stage", stage, "enemyCode", enemyCode, "enemyCount", 3L, "bossCode", bossCode,
                   "statMultiplier", 1.2, "goldPerEnemy", 100L, "bossGold", 500L, "artifactDropRate", 1L,
                   "recommendedPower", 500L, "imageCode", "STAGE_01", "name", "스테이지");
    }

    private static final List<Map<String, Object>> ENEMIES = List.of(enemy("NORMAL", "FALSE"), enemy("BOSS", "TRUE"));

    @Test
    void enemyAndStage_load() {
        GoogleSheetClient sheets = sheets(Map.of(
                ENEMY, ENEMIES,
                STAGE, List.of(stage(2, "NORMAL", "BOSS"), stage(1, "NORMAL", "BOSS"))
        ));

        EnemyTable enemies = new EnemyTable(sheets, ENEMY);
        StageTable stages  = new StageTable(sheets, enemies, STAGE);

        // 시트의 TRUE / FALSE 문자열이 boolean으로 변환됨
        assertTrue(enemies.findByCode("BOSS").orElseThrow().boss());
        assertFalse(enemies.findByCode("NORMAL").orElseThrow().boss());

        // 정렬 후 stage 번호로 조회
        Stage first = stages.findByStage(1).orElseThrow();

        assertEquals(1, first.stage());
        assertEquals(1.0, first.artifactDropRate());    // 정수 칸(1)도 실수로 변환
        assertTrue(stages.findByStage(3).isEmpty());

        // 세부 스테이지 (일반 적 3 + 보스 1)
        assertEquals(4, first.subStageCount());
        assertEquals("NORMAL", first.enemyCodeAt(2));
        assertEquals("BOSS", first.enemyCodeAt(3));
        assertEquals(100, first.killGoldAt(0));
        assertEquals(500, first.killGoldAt(3));
    }

    @Test
    void stage_rejectsWrongEnemy() {
        GoogleSheetClient sheets = sheets(Map.of(
                ENEMY, ENEMIES,
                STAGE, List.of(stage(1, "BOSS", "MISSING"))
        ));

        EnemyTable           enemies = new EnemyTable(sheets, ENEMY);
        IllegalStateException e      = assertThrows(IllegalStateException.class, () -> new StageTable(sheets, enemies, STAGE));

        assertTrue(e.getMessage().contains("enemyCode의 boss 값이 true"), e.getMessage());
        assertTrue(e.getMessage().contains("bossCode가 EnemyTable에 없습니다: MISSING"), e.getMessage());
    }

    @Test
    void stage_rejectsMissingNumber() {
        GoogleSheetClient sheets = sheets(Map.of(
                ENEMY, ENEMIES,
                STAGE, List.of(stage(1, "NORMAL", "BOSS"), stage(3, "NORMAL", "BOSS"))
        ));

        EnemyTable            enemies = new EnemyTable(sheets, ENEMY);
        IllegalStateException e       = assertThrows(IllegalStateException.class, () -> new StageTable(sheets, enemies, STAGE));

        assertTrue(e.getMessage().contains("stage가 존재하지 않습니다: 2"), e.getMessage());
    }

    @Test
    void dropRate_loadAndValidateSum() {
        List<Map<String, Object>> valid = List.of(
                row("grade", "LEGENDARY", "rate", 0.02), row("grade", "COMMON", "rate", 0.6),
                row("grade", "RARE", "rate", 0.28),      row("grade", "EPIC", "rate", 0.1)
        );

        DropRateTable table = new DropRateTable(sheets(Map.of(DROP, valid)), DROP);

        assertEquals(EquipmentGrade.COMMON,    table.getAll().getFirst().grade());
        assertEquals(EquipmentGrade.LEGENDARY, table.getAll().getLast().grade());

        List<Map<String, Object>> invalid = List.of(
                row("grade", "COMMON", "rate", 0.6), row("grade", "RARE", "rate", 0.3)
        );

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> new DropRateTable(sheets(Map.of(DROP, invalid)), DROP));

        assertTrue(e.getMessage().contains("grade가 존재하지 않습니다"), e.getMessage());
        assertTrue(e.getMessage().contains("rate의 합이 1이 아닙니다"), e.getMessage());
    }

    // config 시트 값 (키 존재, 타입 검증은 ConstantTable이 담당)
    private static ConstantTable constants(double critMultiplier, double healRate) {
        ConstantTable constantTable = mock(ConstantTable.class);

        when(constantTable.getDecimal(ConstantKey.CRIT_MULTIPLIER)).thenReturn(critMultiplier);
        when(constantTable.getDecimal(ConstantKey.HEAL_RATE)).thenReturn(healRate);
        when(constantTable.getLong(ConstantKey.NORMAL_BATTLE_TIME_LIMIT)).thenReturn(30L);
        when(constantTable.getLong(ConstantKey.BOSS_BATTLE_TIME_LIMIT)).thenReturn(60L);

        return constantTable;
    }

    @Test
    void battleConfig_readsValues() {
        BattleConfig config = new BattleConfig(constants(1.5, 0.1));

        assertEquals(1.5,  config.getCritMultiplier());
        assertEquals(0.1,  config.getHealRate());
        assertEquals(30.0, config.getNormalTimeLimit());
        assertEquals(60.0, config.getBossTimeLimit());
    }

    @Test
    void battleConfig_invalidValue_fails() {
        IllegalStateException crit = assertThrows(IllegalStateException.class, () -> new BattleConfig(constants(0.5, 0.1)));
        IllegalStateException heal = assertThrows(IllegalStateException.class, () -> new BattleConfig(constants(1.5, 1.5)));

        assertTrue(crit.getMessage().contains("CRIT_MULTIPLIER"), crit.getMessage());
        assertTrue(heal.getMessage().contains("HEAL_RATE"), heal.getMessage());
    }
}
