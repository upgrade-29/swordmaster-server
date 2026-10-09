package com.swordmaster.equipment.artifact.entity;

import com.swordmaster.player.entity.Player;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
// playerId + equipSlot 조합은 유일해야 함 (같은 슬롯에 중복으로 저장할 수 없음)
@Table(
        name              = "player_artifacts",
//        indexes           = @Index(name = "idx_player_artifacts_player_id", columnList = "player_id"),
        uniqueConstraints = @UniqueConstraint(
                name        = "uk_player_artifacts_player_slot",
                columnNames = {"player_id", "equip_slot"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerArtifact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private int level;

    @Column(name = "equip_slot")
    private Integer equipSlot;      // Null 허용 (null 이면 미장착)

    public PlayerArtifact(Player player, String code) {
        this.player = player;
        this.code   = code;
        level       = 0;
        equipSlot   = null;
    }
}
