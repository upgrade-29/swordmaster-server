package com.swordmaster.shop;


import com.swordmaster.common.BusinessException;
import com.swordmaster.common.table.constant.ConstantTable;
import com.swordmaster.currency.CurrencyReason;
import com.swordmaster.currency.CurrencyType;
import com.swordmaster.currency.dto.CurrenciesResponse;
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
import com.swordmaster.gacha.GachaPicker;
import com.swordmaster.gacha.service.GachaService;
import com.swordmaster.gacha.table.GachaCatalog;
import com.swordmaster.gacha.table.GachaEntry;
import com.swordmaster.gacha.table.GachaEntryTable;
import com.swordmaster.idempotency.service.IdempotencyService;
import com.swordmaster.player.entity.Player;
import com.swordmaster.player.repository.PlayerRepository;
import com.swordmaster.player.service.PlayerService;
import com.swordmaster.reward.Reward;
import com.swordmaster.reward.RewardType;
import com.swordmaster.reward.service.RewardService;
import com.swordmaster.shop.dto.ShopPurchaseRequest;
import com.swordmaster.shop.dto.ShopPurchaseResponse;
import com.swordmaster.shop.service.ShopService;
import com.swordmaster.shop.table.ShopCatalog;
import com.swordmaster.shop.table.ShopProduct;
import com.swordmaster.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// 실제 MySQL로 상점 구매의 커밋/롤백을 확인한다
// 테스트 트랜잭션을 끄고(NOT_SUPPORTED) ShopService의 트랜잭션이 실제로 커밋/롤백되게 한다 → 데이터는 @AfterEach에서 직접 삭제
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({ShopService.class, CurrencyService.class, PlayerService.class, RewardService.class,
        GachaService.class, GachaPicker.class, ArtifactService.class})
class ShopPurchaseIntegrationTest {

    @Autowired ShopService                shopService;
    @Autowired CurrencyService            currencyService;
    @Autowired PlayerRepository           playerRepository;
    @Autowired ArtifactRepository         artifactRepository;
    @Autowired EntityManager              em;
    @Autowired PlatformTransactionManager txManager;

    // 구글 시트를 읽는 빈은 가짜로 대체
    @MockitoBean ShopCatalog          shopCatalog;
    @MockitoBean GachaCatalog         gachaCatalog;
    @MockitoBean ArtifactTable        artifactTable;
    @MockitoBean ArtifactEnhanceTable artifactEnhanceTable;  // ArtifactService 의존성
    @MockitoBean ConstantTable        constantTable;         // CurrencyService 의존성
    @MockitoBean SwordTable           swordTable;            // PlayerService 의존성
    @MockitoBean IdempotencyService   idempotencyService;    // 재전송 판별은 범위 밖 → 항상 "처음 온 요청"

    private TransactionTemplate tx;
    private Long userId;
    private Long playerId;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(txManager);
        String unique = UUID.randomUUID().toString().substring(0, 8);

        // 테스트용 유저와 플레이어를 커밋 (골드 0, 다이아 0)
        tx.executeWithoutResult(status -> {
            User user = new User("shop_" + unique + "@test.com", "password");
            em.persist(user);
            Player player = playerRepository.save(new Player(user, "s_" + unique, 0));
            for (CurrencyType type : CurrencyType.values()) em.persist(new PlayerCurrency(player, type));
            em.flush();

            userId   = user.getId();
            playerId = player.getId();
        });

        when(idempotencyService.execute(anyLong(), anyString(), any(), any()))
                .thenAnswer(inv -> inv.<Supplier<?>>getArgument(3).get());
    }

    @AfterEach
    void tearDown() {
        tx.executeWithoutResult(status -> {
            for (String sql : List.of(
                    "DELETE FROM currency_histories WHERE player_id = ?1",
                    "DELETE FROM player_currencies  WHERE player_id = ?1",
                    "DELETE FROM player_artifacts   WHERE player_id = ?1",
                    "DELETE FROM players            WHERE id        = ?1"))
                em.createNativeQuery(sql).setParameter(1, playerId).executeUpdate();

            for (String sql : List.of(
                    "DELETE FROM shop_purchase_logs WHERE user_id = ?1",
                    "DELETE FROM idempotency_keys   WHERE user_id = ?1",
                    "DELETE FROM users              WHERE id      = ?1"))
                em.createNativeQuery(sql).setParameter(1, userId).executeUpdate();
        });
    }

    // ===================== ① 상품 없음 =====================

    @Test
    void 없는_상품이면_PRODUCT_NOT_FOUND이고_재화와_기록은_그대로다() {
        giveDiamond(1000);
        when(shopCatalog.findProduct("NONE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shopService.purchase(userId, request("NONE")))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(e.getCode()).isEqualTo("PRODUCT_NOT_FOUND");
                    assertThat(e.getMessage()).isEqualTo("상품을 찾을 수 없습니다.");
                });

        assertThat(currencies().diamond()).isEqualTo(1000);
        assertThat(shopLogCount()).isZero();
    }

    // ===================== ② 재화 부족 =====================

    @Test
    void 다이아가_부족하면_NOT_ENOUGH_DIAMOND이고_보상과_기록이_없다() {
        product(new ShopProduct("GOLD_SMALL", "골드 주머니", "GOLD", "ICON", CurrencyType.DIAMOND, 100),
                new Reward(RewardType.GOLD, null, 10_000));

        assertThatThrownBy(() -> shopService.purchase(userId, request("GOLD_SMALL")))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getCode()).isEqualTo("NOT_ENOUGH_DIAMOND");
                    assertThat(e.getMessage()).isEqualTo("다이아가 부족합니다.");
                });

        assertThat(currencies().gold()).isZero();
        assertThat(purchaseHistoryCount()).isZero();
        assertThat(shopLogCount()).isZero();
    }

    // ===================== ③ 보상 단계 실패 → 전체 롤백 =====================

    @Test
    void 가챠_개봉이_실패하면_앞서_지급한_골드와_다이아_차감까지_모두_롤백된다() {
        giveDiamond(1000);
        // 1번 보상(골드)은 성공, 2번 보상(가챠)은 시트에 없는 코드라 실패
        product(new ShopProduct("BROKEN", "깨진 상품", "GACHA", "ICON", CurrencyType.DIAMOND, 100),
                new Reward(RewardType.GOLD, null, 10_000),
                new Reward(RewardType.GACHA_BOX, "NO_SUCH_BOX", 1));
        when(gachaCatalog.findPool("NO_SUCH_BOX")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shopService.purchase(userId, request("BROKEN")))
                .isInstanceOf(BusinessException.class);

        CurrenciesResponse after = currencies();
        assertThat(after.diamond()).isEqualTo(1000);    // 차감 취소
        assertThat(after.gold()).isZero();               // 먼저 지급된 골드도 취소
        assertThat(purchaseHistoryCount()).isZero();     // 재화 이력도 남지 않음
        assertThat(shopLogCount()).isZero();             // 구매 기록도 남지 않음
    }

    @Test
    void 아티팩트_지급이_실패해도_전체_롤백된다() {
        giveDiamond(1000);
        product(new ShopProduct("BROKEN_ART", "깨진 아티팩트", "GACHA", "ICON", CurrencyType.DIAMOND, 100),
                new Reward(RewardType.GOLD, null, 10_000),
                new Reward(RewardType.ARTIFACT, "ART_GHOST", 1));
        when(artifactTable.existsByCode("ART_GHOST")).thenReturn(false);

        assertThatThrownBy(() -> shopService.purchase(userId, request("BROKEN_ART")))
                .isInstanceOf(BusinessException.class);

        assertThat(currencies().diamond()).isEqualTo(1000);
        assertThat(currencies().gold()).isZero();
        assertThat(artifacts()).isEmpty();
        assertThat(shopLogCount()).isZero();
    }

    // ===================== ④ 뽑기 정상 지급 =====================

    @Test
    void 골드_상자_10회를_사면_골드_10건이_지급되고_기록된다() {
        giveDiamond(1000);
        // 결과가 하나뿐인 풀 → 항상 골드 100이 나와 결과가 고정된다
        gachaPool("GOLD_BOX", new GachaEntry("GOLD_BOX", RewardType.GOLD, null, 100, 1));
        product(new ShopProduct("GOLD_GACHA_10", "골드 상자 10회", "GACHA", "ICON", CurrencyType.DIAMOND, 1000),
                new Reward(RewardType.GACHA_BOX, "GOLD_BOX", 10));

        ShopPurchaseResponse response = shopService.purchase(userId, request("GOLD_GACHA_10"));

        // 응답: 상자가 아니라 실제로 나온 골드 10건
        assertThat(response.rewards()).hasSize(10)
                .allSatisfy(r -> {
                    assertThat(r.rewardType()).isEqualTo(RewardType.GOLD);
                    assertThat(r.amount()).isEqualTo(100);
                });
        assertThat(response.currencies().gold()).isEqualTo(1000);
        assertThat(response.currencies().diamond()).isZero();

        // DB: 실제로 반영됐는지
        CurrenciesResponse after = currencies();
        assertThat(after.gold()).isEqualTo(1000);
        assertThat(after.diamond()).isZero();
        assertThat(purchaseHistoryCount()).isEqualTo(11);   // 다이아 차감 1 + 골드 지급 10
        assertThat(shopLogCount()).isEqualTo(1);
    }

    @Test
    void 아티팩트_상자에서_같은_아티팩트가_여러번_나오면_한_줄에_재료로_쌓인다() {
        giveDiamond(1000);
        Artifact art = new Artifact(EquipmentGrade.COMMON, EquipmentStat.CRIT_RATE, 0.1, 0.01, "A", "ART_A");
        when(artifactTable.getAll()).thenReturn(List.of(art));
        when(artifactTable.existsByCode("ART_A")).thenReturn(true);
        gachaPool("ART_BOX", new GachaEntry("ART_BOX", RewardType.ARTIFACT, "ART_A", 1, 1));
        product(new ShopProduct("ART_GACHA_3", "아티팩트 상자 3회", "GACHA", "ICON", CurrencyType.DIAMOND, 100),
                new Reward(RewardType.GACHA_BOX, "ART_BOX", 3));

        ShopPurchaseResponse response = shopService.purchase(userId, request("ART_GACHA_3"));

        assertThat(response.rewards()).hasSize(3)
                .allSatisfy(r -> assertThat(r.rewardCode()).isEqualTo("ART_A"));

        List<PlayerArtifact> owned = artifacts();
        assertThat(owned).hasSize(1);                              // 같은 코드는 한 줄
        assertThat(owned.getFirst().getMaterialCount()).isEqualTo(3); // 처음 얻어도 재료 1 → 총 3
        assertThat(currencies().diamond()).isEqualTo(900);
    }

    // ===================== 도우미 =====================

    private ShopPurchaseRequest request(String productCode) {
        return new ShopPurchaseRequest(UUID.randomUUID(), productCode);
    }

    private Player player() {
        return playerRepository.findById(playerId).orElseThrow();
    }

    private void giveDiamond(long amount) {
        tx.executeWithoutResult(s ->
                currencyService.grant(player(), CurrencyType.DIAMOND, amount, CurrencyReason.ADMIN_ADJUST));
    }

    private CurrenciesResponse currencies() {
        return tx.execute(s -> currencyService.getAll(player()));
    }

    private List<PlayerArtifact> artifacts() {
        return tx.execute(s -> artifactRepository.findAllByPlayer(player()));
    }

    private long purchaseHistoryCount() {
        return count("SELECT COUNT(*) FROM currency_histories WHERE player_id = ?1 AND reason = 'PURCHASE'", playerId);
    }

    private long shopLogCount() {
        return count("SELECT COUNT(*) FROM shop_purchase_logs WHERE user_id = ?1", userId);
    }

    private long count(String sql, Long id) {
        return tx.execute(s -> ((Number) em.createNativeQuery(sql).setParameter(1, id).getSingleResult()).longValue());
    }

    private void product(ShopProduct product, Reward... rewards) {
        when(shopCatalog.findProduct(product.productCode())).thenReturn(Optional.of(product));
        when(shopCatalog.findRewards(product.productCode())).thenReturn(List.of(rewards));
    }

    // 실제 GachaCatalog로 풀을 만들어, 가짜 gachaCatalog가 그 풀을 돌려주게 한다
    private void gachaPool(String gachaCode, GachaEntry... entries) {
        GachaEntryTable entryTable = mock(GachaEntryTable.class);
        when(entryTable.gachaCodes()).thenReturn(Set.of(gachaCode));
        when(entryTable.findEntries(gachaCode)).thenReturn(List.of(entries));

        GachaCatalog real = new GachaCatalog(entryTable, artifactTable);
        when(gachaCatalog.findPool(gachaCode)).thenReturn(real.findPool(gachaCode));
    }
}