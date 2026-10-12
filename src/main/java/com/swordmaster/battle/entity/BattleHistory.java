package com.swordmaster.battle.entity;

import com.swordmaster.battle.BattleEndReason;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

// 끝난 전투 기록 (전투당 한 행)
// battleId가 Unique이므로 같은 전투의 보상을 두 번 정산할 수 없음 (Redis 세션 삭제 실패 후 재요청 등)
@Entity
@Immutable      // UPDATE 불가
@Table(
        name              = "battle_histories",
        indexes           = @Index(name = "idx_battle_histories_user", columnList = "user_id, id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_battle_histories_battle_id", columnNames = "battle_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BattleHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "battle_id", nullable = false, length = 32)
    private String battleId;            // Redis 세션의 battleId

    @Column(name = "user_id", nullable = false)
    private Long userId;                // 굳이 조인을 하지 않고 ID 값만 참조

    @Column(nullable = false)
    private int stage;

    @Column(nullable = false)
    private boolean victory;

    @Enumerated(EnumType.STRING)
    @Column(name = "end_reason", nullable = false, length = 20)
    private BattleEndReason endReason;

    @Column(name = "last_sub_stage_index", nullable = false)
    private int lastSubStageIndex;      // 전투가 끝난 세부 스테이지

    @Column(nullable = false)
    private long gold;                  // 지급한 골드 (세션의 accumulatedGold)

    @Column(name = "artifact_codes", length = 500)
    private String artifactCodes;       // 드랍한 아티팩트 코드 (쉼표 구분, 없으면 null)

    @Column(name = "game_data_version", length = 50)
    private String gameDataVersion;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;          // UTC 시각

    @CreationTimestamp
    @Column(name = "ended_at", nullable = false)
    private Instant endedAt;            // UTC 시각

    public BattleHistory(
            String          battleId,
            Long            userId,
            int             stage,
            BattleEndReason endReason,
            int             lastSubStageIndex,
            long            gold,
            String          artifactCodes,
            String          gameDataVersion,
            Instant         startedAt
    ) {
        this.battleId          = battleId;
        this.userId            = userId;
        this.stage             = stage;
        this.victory           = endReason == BattleEndReason.CLEARED;
        this.endReason         = endReason;
        this.lastSubStageIndex = lastSubStageIndex;
        this.gold              = gold;
        this.artifactCodes     = artifactCodes;
        this.gameDataVersion   = gameDataVersion;
        this.startedAt         = startedAt;
    }
}
