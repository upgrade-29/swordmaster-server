package com.swordmaster.shop.entity;


import com.swordmaster.shop.CurrencyType;
import com.swordmaster.shop.dto.Reward;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.List;


//상점에서의 구입 기록을 남기기 위한 테이블 엔티티

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "shop_purchase_logs",
    indexes = {
        @Index(name = "idx_shop_purchase_user",columnList = "user_id,created_at")
}
)
public class ShopPurchaseLogs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id" ,nullable = false)
    private Long userId;

    @Column(name = "product_code",nullable = false ,length = 30)
    private String productCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_type",nullable = false,length = 10)
    private CurrencyType priceType;

    @Column(name = "price",nullable = false)
    private long price;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rewards",nullable = false,columnDefinition = "json")
    private List<Reward> rewards;

    @CreatedDate
    @Column(name = "created_at",nullable = false,columnDefinition = "DATETIME(6)",updatable = false)
    private Instant createdAt;

    public ShopPurchaseLogs(long userId, String productCode, CurrencyType priceType, long price, List<Reward> rewards) {
        this.userId = userId;
        this.productCode = productCode;
        this.priceType = priceType;
        this.price = price;
        this.rewards = List.copyOf(rewards);
    }
}
