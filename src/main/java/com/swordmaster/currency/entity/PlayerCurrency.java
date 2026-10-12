package com.swordmaster.currency.entity;

import com.swordmaster.currency.CurrencyType;
import com.swordmaster.player.entity.Player;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
// playerId + type 조합은 Unique
@Table(
        name              = "player_currencies",
        uniqueConstraints = @UniqueConstraint(
                name        = "uk_player_currencies_player_type",
                columnNames = {"player_id", "type"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerCurrency {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Enumerated(EnumType.STRING)    // 열거형(이름)으로 저장
    @Column(nullable = false, length = 20)
    private CurrencyType type;

    @Column(nullable = false)
    private long amount;

    @Version
    @Column(nullable = false)
    private Long version;       // JPA 버전 관리 (동시 요청)

    public PlayerCurrency(Player player, CurrencyType type) {
        this.player = player;
        this.type   = type;
        amount      = 0;
    }

    // 조회 (유효성 검사)
    public boolean has(long cost) { return amount >= cost; }

    // long 범위를 초과하면 예외
    public void add(long value) { amount = Math.addExact(amount, value); }

    // 호출 여부는 has()로 사전에 체크
    public void subtract(long value) { amount -= value; }
}
