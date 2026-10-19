package com.swordmaster.shop.iap;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

// 구글 결제 영수증 (purchaseToken 한 건당 한 행, 영구 보관)
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name              = "iap_receipts",
        uniqueConstraints = @UniqueConstraint(name = "uk_iap_receipts_token", columnNames = "purchase_token"),
        indexes           = {
                @Index(name = "idx_iap_receipts_status", columnList = "status, created_at"),
                @Index(name = "idx_iap_receipts_user",   columnList = "user_id, created_at")
        }
)
public class IapReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;                // 지급받은 유저 (조인 없이 ID만 참조)

    @Column(name = "product_code", nullable = false, length = 30)
    private String productCode;         // 시트 상품 코드 = Play Console 상품 ID

    @Column(name = "purchase_token", nullable = false, length = 512)
    private String purchaseToken;       // 구글 영수증 (중복 확인 기준)

    @Column(name = "order_id", length = 64)
    private String orderId;             // 구글 주문 번호 (고객 문의 대응용)

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 20)
    private IapReceiptStatus status;

    @Column(name = "purchased_at", nullable = false)
    private Instant purchasedAt;        // 구글 기준 결제 시각 (UTC)

    @Column(name = "consumed_at")
    private Instant consumedAt;         // consume 완료 시각 (미완료면 null)

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;          // 서버가 기록한 시각 (UTC)

    public IapReceipt(Long userId, String productCode, String purchaseToken,
                      String orderId, Instant purchasedAt) {
        this.userId        = userId;
        this.productCode   = productCode;
        this.purchaseToken = purchaseToken;
        this.orderId       = orderId;
        this.purchasedAt   = purchasedAt;
        this.status        = IapReceiptStatus.GRANTED;      // 저장되는 시점 = 지급 직후
    }

    // consume 성공 (GRANTED일 때만 변경, 환불된 영수증을 되돌리지 않음)
    public void markConsumed(Instant now) {
        if (status != IapReceiptStatus.GRANTED) return;

        status     = IapReceiptStatus.CONSUMED;
        consumedAt = now;
    }

    // 구글에서 환불·취소 확인
    public void markRefunded() {
        status = IapReceiptStatus.REFUNDED;
    }
}