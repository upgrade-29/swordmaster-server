package com.swordmaster.shop.repository;

import com.swordmaster.shop.entity.ShopPurchaseLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShopPurchaseLogRepository extends JpaRepository<ShopPurchaseLog, Long> {
    Optional<ShopPurchaseLog> findByUserIdAndRequestId(Long userId, String requestId);
}
