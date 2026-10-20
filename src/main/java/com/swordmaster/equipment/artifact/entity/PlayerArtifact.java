package com.swordmaster.equipment.artifact.entity;

import com.swordmaster.player.entity.Player;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Entity
// playerId + equipSlot 조합은 유일해야 함 (같은 슬롯에 중복으로 저장할 수 없음)
@Table(
        name              = "player_artifacts",
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

    @Column(nullable = false)
    private int materialCount;

    @Column(name = "equip_slot")
    private Integer equipSlot;      // Null 허용 (null 이면 미장착)

    @Version                // 낙관적 락 (보상 지급과 강화 등이 동시에 같은 행을 바꾸면 나중 요청이 실패)
    @Column(nullable = false)
    private long version;

    public PlayerArtifact(Player player, String code) {
        this.player   = player;
        this.code     = code;
        level         = 1;      // 아티팩트 초기 레벨: 0 -> 1로 변경
        materialCount = 0;
        equipSlot     = null;
    }

    public void changeLevel(int level) { this.level = level; }

    public void changeMaterialCount(int materialCount) { this.materialCount = materialCount; }

    public void changeEquipSlot(Integer equipSlot) { this.equipSlot = equipSlot; }
}
