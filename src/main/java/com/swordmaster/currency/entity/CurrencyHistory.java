package com.swordmaster.currency.entity;

import com.swordmaster.currency.CurrencyReason;
import com.swordmaster.currency.CurrencyType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

@Entity
@Immutable      // UPDATE 불가
@Table(
        name    = "currency_histories",
        indexes = @Index(name = "idx_currency_histories_player", columnList = "player_id, type, id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CurrencyHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;          // 굳이 조인을 하지 않고 ID 값만 참조

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CurrencyType type;

    @Column(nullable = false)
    private long amount;            // 변동량

    @Column(name = "balance_after", nullable = false)
    private long balanceAfter;      // 변동 후 잔액

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CurrencyReason reason;

    @Column
    private String memo;             // 관리자 조정 시 사유

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;      // UTC 시각 (세계 표준시)

    public CurrencyHistory(
            Long           playerId,
            CurrencyType   type,
            long           amount,
            long           balanceAfter,
            CurrencyReason reason,
            String         memo
    ) {
        this.playerId     = playerId;
        this.type         = type;
        this.amount       = amount;
        this.balanceAfter = balanceAfter;
        this.reason       = reason;
        this.memo         = memo;
    }
}
