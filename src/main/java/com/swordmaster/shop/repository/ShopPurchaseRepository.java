package com.swordmaster.shop.repository;

import com.swordmaster.shop.entity.ShopPurchaseLogs;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopPurchaseRepository extends JpaRepository<ShopPurchaseLogs, Long> {

}
