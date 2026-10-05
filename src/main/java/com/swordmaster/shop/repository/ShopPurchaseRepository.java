package com.swordmaster.shop.repository;

import com.swordmaster.shop.entity.ShopPurchaseLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopPurchaseRepository extends JpaRepository<ShopPurchaseLog, Long> {

}
