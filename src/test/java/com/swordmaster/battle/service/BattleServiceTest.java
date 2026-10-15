package com.swordmaster.battle.service;

import com.swordmaster.battle.BattleEndReason;
import com.swordmaster.battle.BattleErrorCode;
import com.swordmaster.battle.dto.*;
import com.swordmaster.battle.session.BattleLock;
import com.swordmaster.battle.session.BattleSession;
import com.swordmaster.battle.session.BattleSessionRepository;
import com.swordmaster.battle.simulation.BattleSimulator;
import com.swordmaster.battle.simulation.BattleSubStageResultEnum;
import com.swordmaster.battle.simulation.SubStageResult;
import com.swordmaster.battle.table.BattleConfig;
import com.swordmaster.battle.table.EnemyTable;
import com.swordmaster.battle.table.StageTable;
import com.swordmaster.common.BusinessException;
import com.swordmaster.common.table.constant.ConstantKey;
import com.swordmaster.common.table.constant.ConstantTable;
import com.swordmaster.common.table.GameDataVersion;
import com.swordmaster.currency.dto.CurrenciesResponse;
import com.swordmaster.common.table.GoogleSheetClient;
import com.swordmaster.equipment.sword.dto.SwordResponse;
import com.swordmaster.player.dto.PlayerResponse;
import com.swordmaster.player.service.PlayerFacade;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.junit.jupiter.api.function.Executable;
import tools.jackson.databind.json.JsonMapper;

import java.time.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// BattleService 흐름 테스트 (Redis 세션은 실제 Redis, DB 정산은 mock)
// 실제 Redis(localhost:6379)가 없으면 건너뜀
class BattleServiceTest {
    private static final String VERSION = "v1";
    private static final long   GOLD_PER_ENEMY = 100, BOSS_GOLD = 500;

    // 플레이어가 확실히 이기는 스탯 (일반 적 1마리 + 보스)
    private static final CombatStat PLAYER_STAT = new CombatStat(1000, 1, 10000, 0.2, 0.05);

    private static LettuceConnectionFactory factory;
    private static StringRedisTemplate      redis;

    private final Long         userId    = ThreadLocalRandom.current().nextLong(1_000_000_000L, Long.MAX_VALUE);
    private final Set<String>  battleIds = new HashSet<>();
    private final MutableClock clock     = new MutableClock(Instant.parse("2026-10-09T00:00:00Z"));

    private BattleRewardService battleRewardService;
    private StageTable              stageTable;
    private EnemyTable              enemyTable;
    private BattleConfig            battleConfig;
    private BattleService           service;

    // 테스트에서 시간을 직접 흘리는 시계
    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) { this.now = now; }

        void advance(Duration duration) { now = now.plus(duration); }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }

        @Override public Clock withZone(ZoneId zone) { return this; }

        @Override public Instant instant() { return now; }
    }

    @BeforeAll
    static void connect() {
        factory = new LettuceConnectionFactory(new RedisStandaloneConfiguration("localhost", 6379));
        factory.afterPropertiesSet();
        factory.start();
        redis = new StringRedisTemplate(factory);

        boolean available;

        try {
            available = "PONG".equals(redis.execute(connection -> connection.ping(), true));
        } catch (Exception e) {
            available = false;
        }

        assumeTrue(available, "Redis(localhost:6379)에 연결할 수 없어 테스트를 건너뜁니다.");
    }

    @AfterAll
    static void disconnect() {
        if (factory != null) factory.destroy();
    }

    @BeforeEach
    void setUp() {
        GoogleSheetClient sheets = sheets(Map.of(
                1L, List.of(
                        enemy("NORMAL", "FALSE", 10, 1, 500),
                        enemy("BOSS",   "TRUE",  20, 0.8, 2500)),
                2L, List.of(row("stage", 1L, "enemyCode", "NORMAL", "enemyCount", 1L, "bossCode", "BOSS",
                        "statMultiplier", 1.2, "goldPerEnemy", GOLD_PER_ENEMY, "bossGold", BOSS_GOLD,
                        "artifactDropRate", 0.5, "recommendedPower", 0L, "imageCode", "IMG"))
        ));

        // config 시트 값 (ConstantTable은 모든 키가 있어야 생성되므로 전투에 쓰는 값만 mock)
        ConstantTable constantTable = mock(ConstantTable.class);

        when(constantTable.getDecimal(ConstantKey.CRIT_MULTIPLIER)).thenReturn(1.5);
        when(constantTable.getDecimal(ConstantKey.HEAL_RATE)).thenReturn(0.1);
        when(constantTable.getLong(ConstantKey.NORMAL_BATTLE_TIME_LIMIT)).thenReturn(30L);
        when(constantTable.getLong(ConstantKey.BOSS_BATTLE_TIME_LIMIT)).thenReturn(60L);
        when(constantTable.getText(ConstantKey.X_GAME_DATA_VERSION)).thenReturn(VERSION);

        enemyTable   = new EnemyTable(sheets, 1L);
        stageTable   = new StageTable(sheets, enemyTable, 2L);
        battleConfig = new BattleConfig(constantTable);
        battleRewardService = mock(BattleRewardService.class);

        PlayerFacade                  playerFacade       = mock(PlayerFacade.class);
        CombatStatCalculator          calculator         = mock(CombatStatCalculator.class);

        when(playerFacade.getMe(userId)).thenReturn(new PlayerResponse(        // 검 레벨 0, 클리어한 스테이지 0
                userId, "nick", new SwordResponse(0), new CurrenciesResponse(0, 0), List.of(), new StageProgress(0)));
        when(calculator.calculate(anyInt(), any())).thenReturn(PLAYER_STAT);
        when(battleRewardService.settle(eq(userId), any(), any(), anyInt(), anyLong(), any()))
                .thenAnswer(invocation -> endResponse(invocation.getArgument(2), invocation.getArgument(4)));

        service = new BattleService(
                new BattleSessionRepository(redis, JsonMapper.builder().build(), Duration.ofMinutes(30), Duration.ofMinutes(10)),
                new BattleLock(redis, Duration.ofSeconds(5)),
                battleRewardService, calculator, playerFacade,
                stageTable, enemyTable, battleConfig, new GameDataVersion(constantTable), clock
        );
    }

    @AfterEach
    void cleanUp() {
        List<String> keys = new ArrayList<>(List.of("battle:session:" + userId, "battle:lock:" + userId));

        battleIds.forEach(battleId -> keys.add("battle:end:" + battleId));
        redis.delete(keys);
    }

    // ---------- 시작 / 조회 ----------

    @Test
    void start_createsSession() {
        BattleSessionInfo info = start();

        assertEquals(1, info.stage());
        assertEquals(0, info.subStageIndex());
        assertEquals(PLAYER_STAT.maxHp(), info.playerHp());
        assertEquals(0, info.accumulatedGold());
        assertTrue(info.battleSeed() >= 0);
        assertEquals(32, info.battleId().length());
        assertEquals(info, service.getSession(userId, VERSION));
    }

    @Test
    void start_twice_alreadyInProgress() {
        start();

        assertCode(BattleErrorCode.ALREADY_IN_PROGRESS, () -> service.start(userId, 1, VERSION));
    }

    @Test
    void start_lockedOrMissingStage() {
        assertCode(BattleErrorCode.STAGE_LOCKED, () -> service.start(userId, 2, VERSION));     // clearedStage 0
        assertCode(BattleErrorCode.STAGE_LOCKED, () -> service.start(userId, 0, VERSION));
    }

    @Test
    void gameDataOutdated_keepsSession() {
        BattleSessionInfo info = start();

        assertCode(BattleErrorCode.GAME_DATA_OUTDATED, () -> service.getSession(userId, "old"));
        assertCode(BattleErrorCode.GAME_DATA_OUTDATED, () -> service.report(userId, clientReport(info, 0), "old"));
        assertNotNull(service.getSession(userId, VERSION));
    }

    @Test
    void getSession_none_returnsNull() {
        assertNull(service.getSession(userId, VERSION));
    }

    // ---------- 보고 ----------

    @Test
    void fullBattle_clearedWithGold() {
        BattleSessionInfo info = start();

        // 세부 스테이지 0 (일반 적)
        BattleResultVerifyRequest  first    = clientReport(info, 0);
        BattleResultVerifyResponse response = report(first);

        assertEquals(BattleSubStageResultEnum.ENEMY_DEAD, response.resultEnum());
        assertEquals(1, response.nextSubStageIndex());
        assertEquals(GOLD_PER_ENEMY, response.accumulatedGold());
        assertNull(response.battleEnd());

        // 처치 회복 반영 (최대 체력의 10%, 최대 체력 초과 불가)
        double expectedHp = Math.min(first.playerHp() + PLAYER_STAT.maxHp() * 0.1, PLAYER_STAT.maxHp());

        assertEquals(expectedHp, response.nextPlayerHp());

        // 같은 보고를 다시 보내면 같은 응답
        assertEquals(response, service.report(userId, first, VERSION));

        // 재접속하면 이어서 진행
        BattleSessionInfo resumed = service.getSession(userId, VERSION);

        assertEquals(1, resumed.subStageIndex());
        assertEquals(expectedHp, resumed.playerHp());
        assertEquals(GOLD_PER_ENEMY, resumed.accumulatedGold());

        // 세부 스테이지 1 (보스, 마지막)
        BattleResultVerifyRequest  last = clientReport(resumed, 1);
        BattleResultVerifyResponse end  = report(last);

        assertEquals(1, end.nextSubStageIndex());
        assertEquals(last.playerHp(), end.nextPlayerHp());          // 끝났으면 처치 회복 없음
        assertEquals(GOLD_PER_ENEMY + BOSS_GOLD, end.accumulatedGold());
        assertTrue(end.battleEnd().isVictory());
        verify(battleRewardService).settle(eq(userId), any(), eq(BattleEndReason.CLEARED), eq(1), eq(GOLD_PER_ENEMY + BOSS_GOLD), any());

        // 세션은 삭제, 마지막 보고를 다시 보내면 같은 응답 (다시 정산하지 않음)
        assertNull(service.getSession(userId, VERSION));
        assertEquals(end, service.report(userId, last, VERSION));
        verify(battleRewardService, times(1)).settle(any(), any(), any(), anyInt(), anyLong(), any());
    }

    @Test
    void report_tooFast_verifyFailedAndSettled() {
        BattleSessionInfo info = start();
        BattleResultVerifyRequest request = clientReport(info, 0);

        clock.advance(Duration.ofMillis(100));      // 실제로는 거의 시간이 흐르지 않음

        assertCode(BattleErrorCode.VERIFY_FAILED, () -> service.report(userId, request, VERSION));
        verify(battleRewardService).settle(eq(userId), any(), eq(BattleEndReason.VERIFY_FAILED), eq(0), eq(0L), any());
        assertNull(service.getSession(userId, VERSION));

        // 같은 보고를 다시 보내면 같은 에러 (다시 정산하지 않음)
        assertCode(BattleErrorCode.VERIFY_FAILED, () -> service.report(userId, request, VERSION));
        verify(battleRewardService, times(1)).settle(any(), any(), any(), anyInt(), anyLong(), any());
    }

    @Test
    void report_wrongResult_verifyFailed() {
        BattleSessionInfo         info    = start();
        BattleResultVerifyRequest correct = clientReport(info, 0);
        BattleResultVerifyRequest wrong   = new BattleResultVerifyRequest(
                correct.battleId(), 0, correct.resultEnum(), correct.playerHp() + 1, correct.enemyHp(),
                correct.duration(), correct.playerAttackCount(), correct.enemyAttackCount()
        );

        clock.advance(Duration.ofSeconds(60));

        assertCode(BattleErrorCode.VERIFY_FAILED, () -> service.report(userId, wrong, VERSION));
        assertNull(service.getSession(userId, VERSION));
    }

    // 재접속하면 시간 검증 기준이 다시 시작됨 (그 세부 스테이지를 처음부터 다시 함)
    @Test
    void getSession_restartsTimer() {
        BattleSessionInfo info = start();

        clock.advance(Duration.ofSeconds(60));
        service.getSession(userId, VERSION);

        assertCode(BattleErrorCode.VERIFY_FAILED, () -> service.report(userId, clientReport(info, 0), VERSION));
    }

    @Test
    void report_noSessionOrOtherBattle() {
        BattleSessionInfo         info    = start();
        BattleResultVerifyRequest request = clientReport(info, 0);
        BattleResultVerifyRequest other   = new BattleResultVerifyRequest(
                "other", 0, request.resultEnum(), request.playerHp(), request.enemyHp(),
                request.duration(), request.playerAttackCount(), request.enemyAttackCount()
        );

        assertCode(BattleErrorCode.BATTLE_ID_MISMATCH, () -> service.report(userId, other, VERSION));

        service.abandon(userId, info.battleId());

        assertCode(BattleErrorCode.NO_ACTIVE_BATTLE, () -> service.report(userId, request, VERSION));
    }

    // ---------- 포기 ----------

    @Test
    void abandon_settlesAndDeletes() {
        BattleSessionInfo          info     = start();
        BattleResultVerifyResponse response = report(clientReport(info, 0));

        BattleEndResponse end = service.abandon(userId, info.battleId());

        assertFalse(end.isVictory());
        verify(battleRewardService).settle(eq(userId), any(), eq(BattleEndReason.ABANDONED), eq(1), eq(response.accumulatedGold()), any());
        assertNull(service.getSession(userId, VERSION));

        // 같은 포기를 다시 보내면 같은 응답 (다시 정산하지 않음)
        assertEquals(end, service.abandon(userId, info.battleId()));
        verify(battleRewardService, times(1)).settle(eq(userId), any(), eq(BattleEndReason.ABANDONED), anyInt(), anyLong(), any());
    }

    @Test
    void abandon_otherBattle() {
        start();

        assertCode(BattleErrorCode.BATTLE_ID_MISMATCH, () -> service.abandon(userId, "other"));
    }

    @Test
    void abandon_noSession() {
        assertCode(BattleErrorCode.NO_ACTIVE_BATTLE, () -> service.abandon(userId, "none"));
    }

    // 보고로 끝난 전투는 포기 결과가 아니므로 포기를 다시 받아 주지 않음
    @Test
    void abandon_afterVerifyFailed_noActiveBattle() {
        BattleSessionInfo info = start();

        assertCode(BattleErrorCode.VERIFY_FAILED, () -> service.report(userId, clientReport(info, 0), VERSION));
        assertCode(BattleErrorCode.NO_ACTIVE_BATTLE, () -> service.abandon(userId, info.battleId()));
    }

    // ---------- 도우미 ----------

    private BattleSessionInfo start() {
        BattleSessionInfo info = service.start(userId, 1, VERSION);

        battleIds.add(info.battleId());

        return info;
    }

    // 실제 전투 시간만큼 기다린 뒤 보고 (1배속)
    private BattleResultVerifyResponse report(BattleResultVerifyRequest request) {
        clock.advance(Duration.ofMillis((long) (request.duration() * 1000) + 1));

        return service.report(userId, request, VERSION);
    }

    // 클라와 같은 계산으로 만든 보고
    private BattleResultVerifyRequest clientReport(BattleSessionInfo info, int index) {
        var stage = stageTable.findByStage(info.stage()).orElseThrow();
        var enemy = enemyTable.findByCode(stage.enemyCodeAt(index)).orElseThrow();

        SubStageResult result = battleConfig.getSimulator().simulate(
                info.playerStat(), info.playerHp(),
                BattleSimulator.enemyStat(enemy.baseStat(), stage.statMultiplier()),
                battleConfig.getTimeLimit(enemy), info.battleSeed(), index
        );

        assertEquals(BattleSubStageResultEnum.ENEMY_DEAD, result.resultEnum(), "테스트 스탯은 플레이어가 이기도록 설정");

        return new BattleResultVerifyRequest(
                info.battleId(), index, result.resultEnum(), result.playerHp(), result.enemyHp(),
                result.duration(), result.playerAttackCount(), result.enemyAttackCount()
        );
    }

    private static BattleEndResponse endResponse(BattleEndReason reason, long gold) {
        return new BattleEndResponse(1, reason == BattleEndReason.CLEARED, new BattleRewards(gold, List.of()),
                new CurrenciesResponse(0, 0), new StageProgress(1), List.of(), Instant.EPOCH);
    }

    private static void assertCode(BattleErrorCode code, Executable executable) {
        BusinessException e = assertThrows(BusinessException.class, executable);

        assertEquals(code.name(), e.getCode());
        assertEquals(code.getStatus(), e.getStatus());
    }

    private static GoogleSheetClient sheets(Map<Long, List<Map<String, Object>>> rowsByGid) {
        return new GoogleSheetClient("test") {
            @Override
            public List<Map<String, Object>> readSheet(long gid) { return rowsByGid.get(gid); }
        };
    }

    private static Map<String, Object> enemy(String code, String boss, double attack, double speed, double maxHp) {
        return row("appearanceCode", "APP", "boss", boss, "attack", attack, "attackSpeed", speed, "maxHp", maxHp,
                   "critRate", 0.1, "lifesteal", 0.0, "name", code, "code", code);
    }

    private static Map<String, Object> row(Object... keyValues) {
        Map<String, Object> row = new LinkedHashMap<>();

        for (int i = 0; i < keyValues.length; i += 2) row.put((String) keyValues[i], keyValues[i + 1]);

        return row;
    }
}
