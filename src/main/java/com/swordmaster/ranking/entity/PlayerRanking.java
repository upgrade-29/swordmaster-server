package com.swordmaster.ranking.entity;

import com.swordmaster.player.entity.Player;
import com.swordmaster.ranking.RankingType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(
        name              = "player_rankings",
        uniqueConstraints = @UniqueConstraint(
                name        = "uk_player_rankings_player_type",
                columnNames = {"player_id", "type"}
        ),
        indexes = @Index(
                name       = "idx_player_rankings_rank",
                columnList = "type, score DESC, achieved_at ASC, id ASC"    // 동점 규칙 정렬 순서
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerRanking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)     // Hibernate 6.2 버전 이후부터 Enum 타입 컬럼을 MySQL 전용 타입으로 생성하는 문제에 대해 VARCHAR 타입으로 변경
    @Column(nullable = false, length = 20)
    private RankingType type;

    @Column(nullable = false)
    private long score;     // 최고 기록 (변경 시 무조건 증가해야 함)

    @Column(name = "achieved_at", nullable = false)
    private Instant achievedAt;     // 최고 기록 달성 시각

    // 저장은 rankingRepository.upsertIfHigher로만 함 (생성자로 직접 저장하지 않음)
}
