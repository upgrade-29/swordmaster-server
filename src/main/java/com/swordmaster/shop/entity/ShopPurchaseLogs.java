package com.swordmaster.shop.entity;


import com.swordmaster.shop.Reward;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

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
    //상점에서의 구입 기록을 남기기 위한 테이블 엔티티

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "user_id" ,nullable = false)
    private long userId;

    @Column(name = "product_code",nullable = false ,length = 30)
    private String product_code;

    @Column(name = "price_type",nullable = false,length = 10)
    private String price_type;

    @Column(name = "price",nullable = false)
    private long price;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rewards",nullable = false,columnDefinition = "json")
    private List<Reward> rewards;

    @CreatedDate
    @Column(name = "created_at",nullable = false)
    private LocalDateTime createdAt;  //auditing 추가여부 확인

    public ShopPurchaseLogs(long id, long userId, String product_code, String price_type, long price, List<Reward> rewards, LocalDateTime created_at) {
        this.id = id;
        this.userId = userId;
        this.product_code = product_code;
        this.price_type = price_type;
        this.price = price;
        this.rewards = rewards;
    }
}
