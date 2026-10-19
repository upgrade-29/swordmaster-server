package com.swordmaster.shop.iap;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface IapReceiptRepository extends JpaRepository<IapReceipt, Long> {

    // 이미 처리한 영수증인지 확인 (재전송 대응)
    Optional<IapReceipt> findByPurchaseToken(String purchaseToken);

    // consume 재시도 대상: 지정 시각 이전에 만들어진 특정 상태의 영수증, 오래된 순 100개
    List<IapReceipt> findTop100ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
            IapReceiptStatus status, Instant before);
}