package com.swordmaster.battle.controller;

import com.swordmaster.battle.BattleErrorCode;
import com.swordmaster.battle.dto.BattleEndResponse;
import com.swordmaster.battle.dto.BattleResultVerifyRequest;
import com.swordmaster.battle.dto.BattleResultVerifyResponse;
import com.swordmaster.battle.dto.BattleRewards;
import com.swordmaster.battle.dto.BattleSessionInfo;
import com.swordmaster.battle.dto.CombatStat;
import com.swordmaster.battle.dto.StageProgress;
import com.swordmaster.battle.service.BattleService;
import com.swordmaster.battle.simulation.BattleSubStageResultEnum;
import com.swordmaster.common.GlobalExceptionHandler;
import com.swordmaster.currency.CurrencyType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// 요청 매핑, 헤더/본문 전달, 에러 응답 형식 확인 (전투 로직은 BattleServiceTest)
class BattleControllerTest {
    private static final Long    USER_ID = 7L;
    private static final String  VERSION = "v1";
    private static final Instant NOW     = Instant.parse("2026-10-09T00:00:00Z");

    private static final BattleSessionInfo SESSION = new BattleSessionInfo(
            "abc", 2, 123L, new CombatStat(10, 1.5, 100, 0.1, 0), 0, 100, 0, NOW
    );

    private final BattleService battleService = mock(BattleService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(USER_ID, null, List.of()));

        mockMvc = MockMvcBuilders.standaloneSetup(new BattleController(battleService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getSession_passesHeaderVersion() throws Exception {
        when(battleService.getSession(USER_ID, VERSION)).thenReturn(SESSION);

        mockMvc.perform(get("/api/battle/session").header(BattleController.GAME_DATA_VERSION_HEADER, VERSION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.battleId").value("abc"))
                .andExpect(jsonPath("$.battleSeed").value(123))
                .andExpect(jsonPath("$.playerStat.attackSpeed").value(1.5));
    }

    @Test
    void getSession_noSession_emptyBody() throws Exception {
        when(battleService.getSession(USER_ID, VERSION)).thenReturn(null);

        mockMvc.perform(get("/api/battle/session").header(BattleController.GAME_DATA_VERSION_HEADER, VERSION))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    // 헤더가 없으면 null로 넘겨 서비스가 버전 불일치로 판단
    @Test
    void getSession_noHeader_passesNull() throws Exception {
        when(battleService.getSession(USER_ID, null)).thenThrow(BattleErrorCode.GAME_DATA_OUTDATED.exception());

        mockMvc.perform(get("/api/battle/session"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("GAME_DATA_OUTDATED"));
    }

    @Test
    void start() throws Exception {
        when(battleService.start(USER_ID, 2, VERSION)).thenReturn(SESSION);

        mockMvc.perform(post("/api/battles")
                        .header(BattleController.GAME_DATA_VERSION_HEADER, VERSION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stage\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value(2));
    }

    @Test
    void start_invalidStage_400WithoutCode() throws Exception {
        mockMvc.perform(post("/api/battles")
                        .header(BattleController.GAME_DATA_VERSION_HEADER, VERSION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stage\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("stage는 1 이상이어야 합니다."))
                .andExpect(jsonPath("$.code").doesNotExist());

        verifyNoInteractions(battleService);
    }

    @Test
    void start_alreadyInProgress() throws Exception {
        when(battleService.start(USER_ID, 2, VERSION)).thenThrow(BattleErrorCode.ALREADY_IN_PROGRESS.exception());

        mockMvc.perform(post("/api/battles")
                        .header(BattleController.GAME_DATA_VERSION_HEADER, VERSION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stage\":2}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_IN_PROGRESS"));
    }

    @Test
    void report() throws Exception {
        BattleResultVerifyRequest request = new BattleResultVerifyRequest(
                "abc", VERSION, 0, BattleSubStageResultEnum.ENEMY_DEAD, 80.5, 0, 12.3, 18, 10
        );

        when(battleService.report(USER_ID, request)).thenReturn(new BattleResultVerifyResponse(
                0, BattleSubStageResultEnum.ENEMY_DEAD, 1, 90.5, 30, null
        ));

        mockMvc.perform(post("/api/battle/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"battleId":"abc","gameDataVersion":"v1","subStageIndex":0,"resultEnum":"ENEMY_DEAD",
                                 "playerHp":80.5,"enemyHp":0,"duration":12.3,"playerAttackCount":18,"enemyAttackCount":10}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextSubStageIndex").value(1))
                .andExpect(jsonPath("$.resultEnum").value("ENEMY_DEAD"))
                .andExpect(jsonPath("$.battleEnd").doesNotExist());
    }

    @Test
    void report_missingBattleId_400() throws Exception {
        mockMvc.perform(post("/api/battle/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"gameDataVersion":"v1","subStageIndex":0,"resultEnum":"ENEMY_DEAD",
                                 "playerHp":80.5,"enemyHp":0,"duration":12.3,"playerAttackCount":18,"enemyAttackCount":10}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("battleId가 비어 있습니다."));

        verifyNoInteractions(battleService);
    }

    @Test
    void report_verifyFailed() throws Exception {
        when(battleService.report(eq(USER_ID), any())).thenThrow(BattleErrorCode.VERIFY_FAILED.exception());

        mockMvc.perform(post("/api/battle/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"battleId":"abc","gameDataVersion":"v1","subStageIndex":0,"resultEnum":"ENEMY_DEAD",
                                 "playerHp":80.5,"enemyHp":0,"duration":12.3,"playerAttackCount":18,"enemyAttackCount":10}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VERIFY_FAILED"));
    }

    @Test
    void abandon() throws Exception {
        when(battleService.abandon(USER_ID, "abc")).thenReturn(new BattleEndResponse(
                2, false, new BattleRewards(30, List.of()), Map.of(CurrencyType.GOLD, 130L),
                new StageProgress(1), List.of(), NOW
        ));

        mockMvc.perform(post("/api/battle/abandon")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"battleId\":\"abc\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isVictory").value(false))
                .andExpect(jsonPath("$.rewards.gold").value(30))
                .andExpect(jsonPath("$.currencies.GOLD").value(130))
                .andExpect(jsonPath("$.stageProgress.clearedStage").value(1));
    }

    @Test
    void abandon_noActiveBattle() throws Exception {
        when(battleService.abandon(USER_ID, "abc")).thenThrow(BattleErrorCode.NO_ACTIVE_BATTLE.exception());

        mockMvc.perform(post("/api/battle/abandon")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"battleId\":\"abc\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NO_ACTIVE_BATTLE"));
    }
}
