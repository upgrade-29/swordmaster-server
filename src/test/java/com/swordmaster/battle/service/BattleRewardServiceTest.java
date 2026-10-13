package com.swordmaster.battle.service;

import com.swordmaster.battle.BattleEndReason;
import com.swordmaster.battle.dto.BattleEndResponse;
import com.swordmaster.battle.dto.CombatStat;
import com.swordmaster.battle.entity.BattleHistory;
import com.swordmaster.battle.repository.BattleHistoryRepository;
import com.swordmaster.battle.session.BattleSession;
import com.swordmaster.battle.table.DropRate;
import com.swordmaster.battle.table.DropRateTable;
import com.swordmaster.battle.table.Stage;
import com.swordmaster.battle.table.StageTable;
import com.swordmaster.common.table.constant.ConstantTable;
import com.swordmaster.currency.CurrencyReason;
import com.swordmaster.currency.CurrencyType;
import com.swordmaster.currency.entity.CurrencyHistory;
import com.swordmaster.currency.entity.PlayerCurrency;
import com.swordmaster.currency.service.CurrencyService;
import com.swordmaster.equipment.EquipmentGrade;
import com.swordmaster.equipment.EquipmentStat;
import com.swordmaster.equipment.artifact.entity.PlayerArtifact;
import com.swordmaster.equipment.artifact.repository.ArtifactRepository;
import com.swordmaster.equipment.artifact.service.ArtifactService;
import com.swordmaster.equipment.artifact.table.Artifact;
import com.swordmaster.equipment.artifact.table.ArtifactEnhanceTable;
import com.swordmaster.equipment.artifact.table.ArtifactTable;
import com.swordmaster.equipment.sword.table.SwordTable;
import com.swordmaster.idempotency.service.IdempotencyService;
import com.swordmaster.player.entity.Player;
import com.swordmaster.player.repository.PlayerRepository;
import com.swordmaster.player.service.PlayerService;
import com.swordmaster.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

// 실제 MySQL로 정산 확인 (테스트마다 롤백)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // 내장 DB 대신 실제 MySQL 사용
@Import({BattleRewardService.class, CurrencyService.class, PlayerService.class, ArtifactService.class})
class BattleRewardServiceTest {
    private static final Instant    NOW  = Instant.parse("2026-10-09T00:00:00Z");
    private static final CombatStat STAT = new CombatStat(100, 1, 1000, 0, 0);

    @Autowired BattleRewardService           battleRewardService;
    @Autowired CurrencyService               currencyService;
    @Autowired PlayerRepository              playerRepository;
    @Autowired ArtifactRepository            artifactRepository;
    @Autowired BattleHistoryRepository       battleHistoryRepository;
    @Autowired EntityManager                 em;

    @MockitoBean StageTable    stageTable;
    @MockitoBean DropRateTable dropRateTable;
    @MockitoBean ArtifactTable artifactTable;
    @MockitoBean ConstantTable constantTable;     // CurrencyService 의존성
    @MockitoBean SwordTable    swordTable;        // PlayerService 의존성
    @MockitoBean ArtifactEnhanceTable artifactEnhanceTable;   // ArtifactService 의존성
    @MockitoBean IdempotencyService   idempotencyService;

    private User   user;
    private Player player;

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString().substring(0, 8);

        user   = new User("battle_" + unique + "@test.com", "password");
        em.persist(user);
        player = playerRepository.save(new Player(user, 0));
        // 골드 0, 다이아 0으로 시작 (CurrencyService.createAll은 시작 골드를 지급하므로 직접 생성)
        for (CurrencyType type : CurrencyType.values()) em.persist(new PlayerCurrency(player, type));
        em.flush();

        // 드랍 확률 100%, COMMON 100%, COMMON 아티팩트 하나 -> 승리하면 항상 ART_A 드랍
        when(stageTable.findByStage(anyInt())).thenAnswer(invocation -> Optional.of(stage(invocation.getArgument(0), 1.0)));
        when(dropRateTable.getAll()).thenReturn(List.of(
                new DropRate(EquipmentGrade.COMMON, 1.0), new DropRate(EquipmentGrade.RARE, 0),
                new DropRate(EquipmentGrade.EPIC, 0),     new DropRate(EquipmentGrade.LEGENDARY, 0)));
        when(artifactTable.getAll()).thenReturn(List.of(
                new Artifact(EquipmentGrade.COMMON, EquipmentStat.CRIT_RATE, 0.1, 0.01, "A", "ART_A")));
        when(artifactTable.existsByCode("ART_A")).thenReturn(true);
    }

    // ---------- 스키마 (ddl-auto=update는 제약 생성에 실패해도 로그만 남기므로 직접 확인) ----------

    @Test
    void schema_created() {
        assertEquals(1, count("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'players' AND column_name = 'cleared_stage'"));
        assertEquals(1, count("SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics WHERE table_schema = DATABASE() "
                + "AND table_name = 'battle_histories' AND index_name = 'uk_battle_histories_battle_id'"));
    }

    // ---------- 정산 ----------

    @Test
    void cleared_grantsGoldClearsStageAndDrops() {
        BattleEndResponse end = settle(session(1), BattleEndReason.CLEARED, 600);

        assertTrue(end.isVictory());
        assertEquals(600, end.rewards().gold());
        assertEquals(List.of("ART_A"), end.rewards().artifactCodes());
        assertEquals(600L, end.currencies().gold());
        assertEquals(1, end.stageProgress().clearedStage());
        assertEquals(1, end.changedArtifacts().size());
        assertEquals(0, end.changedArtifacts().getFirst().level());
        assertEquals(1, end.changedArtifacts().getFirst().materialCount());    // 처음 얻어도 재료 1

        em.flush();
        em.clear();

        assertEquals(1, clearedStage());

        List<PlayerArtifact> artifacts = artifactRepository.findAllByPlayer(player);

        assertEquals(1, artifacts.size());
        assertEquals("ART_A", artifacts.getFirst().getCode());
        assertEquals(0, artifacts.getFirst().getLevel());
        assertEquals(1, artifacts.getFirst().getMaterialCount());
        assertNull(artifacts.getFirst().getEquipSlot());

        CurrencyHistory history = em.createQuery(
                        "SELECT h FROM CurrencyHistory h WHERE h.playerId = :id AND h.reason = :reason", CurrencyHistory.class)
                .setParameter("id", player.getId())
                .setParameter("reason", CurrencyReason.REWARD)
                .getSingleResult();

        assertEquals(CurrencyReason.REWARD, history.getReason());
        assertEquals(600, history.getAmount());
    }

    @Test
    void cleared_sameArtifactAgain_addsMaterial() {
        settle(session(1), BattleEndReason.CLEARED, 0);

        BattleEndResponse second = settle(session(1), BattleEndReason.CLEARED, 0);

        assertEquals(List.of("ART_A"), second.rewards().artifactCodes());
        assertEquals(2, second.changedArtifacts().getFirst().materialCount());

        em.flush();
        em.clear();

        List<PlayerArtifact> artifacts = artifactRepository.findAllByPlayer(player);

        assertEquals(1, artifacts.size());      // 새 행이 생기지 않음
        assertEquals(2, artifacts.getFirst().getMaterialCount());
    }

    @Test
    void defeat_grantsGoldOnly() {
        BattleEndResponse end = settle(session(1), BattleEndReason.PLAYER_DEAD, 200);

        assertFalse(end.isVictory());
        assertEquals(200L, end.currencies().gold());
        assertEquals(0, end.stageProgress().clearedStage());
        assertTrue(end.rewards().artifactCodes().isEmpty());
        assertTrue(end.changedArtifacts().isEmpty());

        em.flush();
        em.clear();

        assertEquals(0, clearedStage());
        assertTrue(artifactRepository.findAllByPlayer(player).isEmpty());
    }

    @Test
    void noGold_noCurrencyChange() {
        BattleEndResponse end = settle(session(1), BattleEndReason.ABANDONED, 0);

        assertEquals(0L, end.currencies().gold());
    }

    // 낮은 스테이지를 다시 깨도 최고 기록은 그대로
    @Test
    void clearedStage_onlyIncreases() {
        settle(session(1), BattleEndReason.CLEARED, 0);
        settle(session(2), BattleEndReason.CLEARED, 0);

        BattleEndResponse replay = settle(session(1), BattleEndReason.CLEARED, 0);

        assertEquals(2, replay.stageProgress().clearedStage());

        em.flush();
        em.clear();

        assertEquals(2, clearedStage());
    }

    // 같은 전투를 다시 정산하면 보상을 주지 않음
    @Test
    void sameBattle_settledOnce() {
        BattleSession session = session(1);

        settle(session, BattleEndReason.CLEARED, 600);

        BattleEndResponse again = settle(session, BattleEndReason.CLEARED, 600);

        assertEquals(600L, again.currencies().gold());     // 두 번 지급되지 않음
        assertTrue(again.changedArtifacts().isEmpty());

        em.flush();
        em.clear();

        assertEquals(1, battleHistoryRepository.findAll().stream()
                .filter(history -> history.getBattleId().equals(session.battleId())).count());
        assertEquals(1, artifactRepository.findAllByPlayer(player).size());
        assertEquals(1, artifactRepository.findAllByPlayer(player).getFirst().getMaterialCount());   // 재료도 한 번만 쌓임
    }

    @Test
    void history_saved() {
        BattleSession session = session(1);

        settle(session, BattleEndReason.VERIFY_FAILED, 100);
        em.flush();
        em.clear();

        BattleHistory history = battleHistoryRepository.findAll().stream()
                .filter(h -> h.getBattleId().equals(session.battleId()))
                .findFirst()
                .orElseThrow();

        assertEquals(user.getId(), history.getUserId());
        assertEquals(BattleEndReason.VERIFY_FAILED, history.getEndReason());
        assertFalse(history.isVictory());
        assertEquals(100, history.getGold());
        assertEquals(2, history.getLastSubStageIndex());
        assertEquals("v1", history.getGameDataVersion());
        assertNull(history.getArtifactCodes());
        assertNotNull(history.getEndedAt());
    }

    // ---------- 도우미 ----------

    // 실제로는 정산 1회가 트랜잭션 1개이므로, 호출마다 영속성 컨텍스트를 비워 DB에서 새로 읽게 함
    // (테스트 전체가 트랜잭션 하나라 벌크 UPDATE한 clearedStage가 캐시된 Player에 반영되지 않음)
    private BattleEndResponse settle(BattleSession session, BattleEndReason reason, long gold) {
        em.flush();
        em.clear();

        return battleRewardService.settle(user.getId(), session, reason, 2, gold, NOW);
    }

    // DB에 저장된 진행도
    private int clearedStage() {
        em.clear();

        return playerRepository.findById(player.getId()).orElseThrow().getClearedStage();
    }

    // 검 강화처럼 Player를 엔티티로 저장해도 전투에서 갱신한 clearedStage를 덮어쓰지 않음 (@DynamicUpdate)
    @Test
    void swordLevelChange_keepsClearedStage() {
        Player stale = playerRepository.findById(player.getId()).orElseThrow();     // 클리어 전에 읽어 둔 Player

        em.detach(stale);
        settle(session(1), BattleEndReason.CLEARED, 0);

        stale.changeSwordLevel(5);
        em.merge(stale);
        em.flush();

        assertEquals(1, clearedStage());
        assertEquals(5, playerRepository.findById(player.getId()).orElseThrow().getSwordLevel());
    }

    private static BattleSession session(int stage) {
        return BattleSession.start(UUID.randomUUID().toString().replace("-", ""), stage, 1L, STAT, "v1", NOW);
    }

    private static Stage stage(int stage, double dropRate) {
        return new Stage(stage, "NORMAL", 3, "BOSS", 1, 100, 500, dropRate, 0, "IMG");
    }

    private long count(String sql) {
        return ((Number) em.createNativeQuery(sql).getSingleResult()).longValue();
    }
}
