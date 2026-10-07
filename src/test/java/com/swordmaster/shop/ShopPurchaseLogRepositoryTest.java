package com.swordmaster.shop;


import com.swordmaster.shop.dto.Currencies;
import com.swordmaster.shop.dto.Reward;
import com.swordmaster.shop.dto.ShopPurchaseResponse;
import com.swordmaster.shop.entity.ShopPurchaseLog;
import com.swordmaster.shop.repository.ShopPurchaseLogRepository;
import com.swordmaster.shop.table.ShopProduct;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // 내장 DB 대신 실제 MySQL 사용
class ShopPurchaseLogRepositoryTest {

    @Autowired
    ShopPurchaseLogRepository repository;
    @Autowired EntityManager em;

    private static final ShopProduct PRODUCT =
            new ShopProduct("TEST_001", "테스트 상품", "TEST", "icon_test", CurrencyType.GOLD, 100L);
    private static final List<Reward> REWARDS = List.of(
            new Reward(RewardType.GOLD, null, 500L),
            new Reward(RewardType.ITEM, "ITEM_001", 1L));
    private static final ShopPurchaseResponse RESPONSE =
            new ShopPurchaseResponse(REWARDS, new Currencies(900L, 10));

    private ShopPurchaseLog newLog(Long userId, UUID requestId) {
        return new ShopPurchaseLog(userId, requestId, PRODUCT, REWARDS, RESPONSE);
    }

    @Test
    void 저장후_조회하면_모든_값이_그대로_돌아온다() {
        UUID requestId = UUID.randomUUID();
        repository.saveAndFlush(newLog(1L, requestId));
        em.clear(); // 1차 캐시를 비워서 DB에서 다시 읽게 함

        Optional<ShopPurchaseLog> found = repository.findByUserIdAndRequestId(1L, requestId.toString());

        assertThat(found).isPresent();
        ShopPurchaseLog log = found.get();
        assertThat(log.getProductCode()).isEqualTo("TEST_001");
        assertThat(log.getPriceType()).isEqualTo(CurrencyType.GOLD);
        assertThat(log.getPrice()).isEqualTo(100L);
        assertThat(log.getRewards()).isEqualTo(REWARDS);
        assertThat(log.getResponseBody()).isEqualTo(RESPONSE);
        assertThat(log.getCreatedAt()).isNotNull(); // Auditing 동작 확인
    }

    @Test
    void response_body는_JSON_객체로_저장된다() {
        UUID requestId = UUID.randomUUID();
        repository.saveAndFlush(newLog(1L, requestId));

        Object type = em.createNativeQuery(
                        "SELECT JSON_TYPE(response_body) FROM shop_purchase_logs WHERE request_id = :rid")
                .setParameter("rid", requestId.toString())
                .getSingleResult();

        assertThat(String.valueOf(type)).isEqualTo("OBJECT"); // 이중 인코딩이면 "STRING"
    }

    @Test
    void userId나_requestId가_다르면_조회되지_않는다() {
        UUID requestId = UUID.randomUUID();
        repository.saveAndFlush(newLog(1L, requestId));

        assertThat(repository.findByUserIdAndRequestId(2L, requestId.toString())).isEmpty();
        assertThat(repository.findByUserIdAndRequestId(1L, UUID.randomUUID().toString())).isEmpty();
    }

    @Test
    void 같은_유저_같은_requestId는_두번_저장할_수_없다() {
        UUID requestId = UUID.randomUUID();
        repository.saveAndFlush(newLog(1L, requestId));

        assertThatThrownBy(() -> repository.saveAndFlush(newLog(1L, requestId)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 다른_유저라면_같은_requestId도_저장된다() {
        UUID requestId = UUID.randomUUID();
        repository.saveAndFlush(newLog(1L, requestId));

        assertThatCode(() -> repository.saveAndFlush(newLog(2L, requestId)))
                .doesNotThrowAnyException();
    }
}