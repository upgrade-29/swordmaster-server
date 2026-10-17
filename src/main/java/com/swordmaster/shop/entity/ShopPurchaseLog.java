package com.swordmaster.shop.entity;


import com.swordmaster.reward.Reward;
import com.swordmaster.shop.PriceType;
import com.swordmaster.shop.dto.ShopPurchaseResponse;
import com.swordmaster.shop.table.ShopProduct;
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
import java.util.UUID;


//상점에서의 완료된 구입 기록을 저장하는 용도의 엔티티

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "shop_purchase_logs",
        indexes = {
                @Index(name = "idx_shop_purchase_user", columnList = "user_id,created_at"),
                @Index(name = "idx_shop_purchase_user_request", columnList = "user_id,request_id")
        }
)
public class ShopPurchaseLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId; //구매한 유저의 id

    @Column(name = "request_id", nullable = false, columnDefinition = "CHAR(36)")
    private String requestId; //구매 요청의 UUID

    @Column(name = "product_code", nullable = false, length = 30)
    private String productCode; //구매한 상품의 코드

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "price_type", nullable = false, length = 10)
    private PriceType priceType; //구매에 사용된 결제 방식

    @Column(name = "price", nullable = false)
    private long price; //상품 구매에 사용한 재화량

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_body", nullable = false, columnDefinition = "json")
    private ShopPurchaseResponse responseBody; //클라이언트에게 돌아간 응답

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rewards", nullable = false, columnDefinition = "json")
    private List<Reward> rewards; //플레이어에게 지급된 보상 목록

    @CreatedDate
    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(6)", updatable = false)
    private Instant createdAt;

    public ShopPurchaseLog(Long userId, UUID requestId, ShopProduct product,
                           List<Reward> rewards, ShopPurchaseResponse responseBody) {

        this.userId = userId;
        this.requestId = requestId.toString();
        this.productCode = product.productCode();
        this.priceType = product.priceType();
        this.price = product.price();
        this.rewards = List.copyOf(rewards);
        this.responseBody = responseBody;
    }
}
