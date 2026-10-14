package com.swordmaster.ranking.repository;

import com.swordmaster.ranking.RankingType;
import com.swordmaster.ranking.entity.PlayerRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RankingRepository extends JpaRepository<PlayerRanking, Long> {
    // 커밋 후 Redis 반영용 (플레이어 ID로 조회)
    Optional<PlayerRanking> findByPlayer_IdAndType(Long playerId, RankingType type);

    // Redis 복구용 (종류별 전체 기록)
    List<PlayerRanking> findAllByType(RankingType type);

    // 기록에 없으면 새로 저장, 있으면 기존 기록보다 높을 때만 갱신
    // achieved_at을 score 보다 먼저 갱신해야 갱신 전 score와 비교함
    @Modifying
    @Query(value = """
        INSERT INTO player_rankings (player_id, type, score, achieved_at)
        VALUES (:playerId, :type, :score, :now) AS new
        ON DUPLICATE KEY UPDATE
            achieved_at = IF(new.score > player_rankings.score, new.achieved_at, player_rankings.achieved_at),
            score       = GREATEST(player_rankings.score, new.score)
        """, nativeQuery = true)
    void upsertIfHigher(
            @Param("playerId") Long    playerId,
            @Param("type")     String  type,
            @Param("score")    long    score,
            @Param("now")      Instant now
            );
}
