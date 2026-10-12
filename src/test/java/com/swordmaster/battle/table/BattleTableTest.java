package com.swordmaster.battle.table;

import com.swordmaster.common.table.ConfigTable;
import com.swordmaster.common.table.GoogleSheetClient;
import com.swordmaster.equipment.EquipmentGrade;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// 시트 대신 고정된 행을 돌려주는 GoogleSheetClient로 테이블 변환 / 검증을 확인
// (GoogleSheetClient는 숫자를 Long / Double, 나머지는 String으로 넘겨주므로 같은 형태로 입력)
class BattleTableTest {
    private static final long ENEMY = 1, STAGE = 2, DROP = 3, CONFIG = 4;

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
                   "recommendedPower", 500L, "imageCode", "STAGE_01");
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

    @Test
    void battleConfig_readsValues() {
        List<Map<String, Object>> rows = List.of(
                row("key", "CRIT_MULTIPLIER",          "value", 1.5,  "description", "치명타 배율"),
                row("key", "NORMAL_BATTLE_TIME_LIMIT", "value", 30L,  "description", "일반"),
                row("key", "BOSS_BATTLE_TIME_LIMIT",   "value", 60L,  "description", "보스"),
                row("key", "HEAL_RATE",                "value", 0.1,  "description", "회복"),
                row("key", "STARTER_ARTIFACTS",        "value", "CRIT_01,HP_01", "description", null)
        );

        ConfigTable  configTable = new ConfigTable(sheets(Map.of(CONFIG, rows)), CONFIG);
        BattleConfig config      = new BattleConfig(configTable);

        assertEquals(1.5,  config.getCritMultiplier());
        assertEquals(0.1,  config.getHealRate());
        assertEquals(30.0, config.getNormalTimeLimit());    // 정수 칸도 실수로 읽음
        assertEquals(60.0, config.getBossTimeLimit());
        assertEquals("CRIT_01,HP_01", configTable.getText("STARTER_ARTIFACTS"));
        assertEquals(30, configTable.getLong("NORMAL_BATTLE_TIME_LIMIT"));
        assertThrows(IllegalStateException.class, () -> configTable.getLong("CRIT_MULTIPLIER"));
    }

    @Test
    void battleConfig_missingKey_fails() {
        List<Map<String, Object>> rows = List.of(
                row("key", "CRIT_MULTIPLIER", "value", 1.5, "description", "치명타 배율")
        );

        ConfigTable           configTable = new ConfigTable(sheets(Map.of(CONFIG, rows)), CONFIG);
        IllegalStateException e           = assertThrows(IllegalStateException.class, () -> new BattleConfig(configTable));

        assertTrue(e.getMessage().contains("HEAL_RATE 키가 존재하지 않습니다"), e.getMessage());
    }
}
