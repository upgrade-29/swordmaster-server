package com.swordmaster.battle.session;

import com.swordmaster.battle.dto.BattleEndResponse;
import com.swordmaster.battle.dto.BattleResultVerifyResponse;
import com.swordmaster.battle.dto.BattleRewards;
import com.swordmaster.battle.dto.CombatStat;
import com.swordmaster.battle.dto.StageProgress;
import com.swordmaster.battle.simulation.BattleSubStageResultEnum;
import com.swordmaster.currency.dto.CurrenciesResponse;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Redis에 JSON 문자열로 저장되므로 저장 -> 조회 시 값이 그대로 복원되는지 확인
class BattleSessionJsonTest {
    private final JsonMapper mapper = JsonMapper.builder().build();

    private static final CombatStat STAT = new CombatStat(245.0, 1.35, 3200.0, 0.18, 0.05);
    private static final Instant    NOW  = Instant.parse("2026-10-08T03:12:45.123Z");

    @Test
    void start_roundTrip() {
        BattleSession session = BattleSession.start("battle-1", 12, 8412093746512038871L, STAT, "1.0.3", NOW);

        BattleSession restored = mapper.readValue(mapper.writeValueAsString(session), BattleSession.class);

        assertEquals(session, restored);
        assertEquals(0, restored.subStageIndex());
        assertEquals(STAT.maxHp(), restored.playerHp());
        assertNull(restored.lastReport());
    }

    @Test
    void advance_roundTrip_withLastReport() {
        BattleResultVerifyResponse response = new BattleResultVerifyResponse(
                0, BattleSubStageResultEnum.ENEMY_DEAD, 1, 3036.4, 120, null
        );

        BattleSession session = BattleSession.start("battle-1", 12, 1L, STAT, "1.0.3", NOW)
                .advance(3036.4, 120, NOW.plusSeconds(8), response);

        BattleSession restored = mapper.readValue(mapper.writeValueAsString(session), BattleSession.class);

        assertEquals(session, restored);
        assertEquals(1, restored.subStageIndex());
        assertEquals(0, restored.lastReport().subStageIndex());
        assertEquals(response, restored.lastReport().response());
    }

    @Test
    void endResult_roundTrip() {
        BattleEndResponse end = new BattleEndResponse(
                12,
                true,
                new BattleRewards(480, List.of("ART_FLAME_RING")),
                new CurrenciesResponse(15480, 120),
                new StageProgress(12),
                List.of(),
                NOW
        );
        BattleResultVerifyResponse response = new BattleResultVerifyResponse(
                3, BattleSubStageResultEnum.ENEMY_DEAD, 3, 2800.0, 480, end
        );

        String                     json     = mapper.writeValueAsString(response);
        BattleResultVerifyResponse restored = mapper.readValue(json, BattleResultVerifyResponse.class);

        assertEquals(response, restored);
        assertTrue(json.contains("\"isVictory\":true"), json);
        assertTrue(json.contains("\"gold\":15480"), json);
    }
}
