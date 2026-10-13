package com.swordmaster.ranking.repository;

import com.swordmaster.player.entity.Player;
import com.swordmaster.ranking.RankingType;
import com.swordmaster.ranking.entity.PlayerRanking;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RankingRepository extends JpaRepository<PlayerRanking, Long> {
    Optional<PlayerRanking> findByPlayerAndType(Player player, RankingType type);

    long countByType(RankingType type);

    // 기록에 없으면 새로 저장, 있으면 기존 기록보다 높을 때만 갱신
    // achieved_at을 score 보다 먼저 계산해야 갱신 전 score와 비교함
    @Modifying
    @Query(value = """
        INSERT INTO player_rankings (player_id, type, score, achieved_at)
        VALUES (:playerId, :type, :score, :now) AS new
        ON DUPLICATE KEY UPDATE
            achieved_at = IF(new.score > player_rankings.score, new.achieved_at, player_rankings.achieved_at),
            score       = GREATEST(player_rankings.score, new.score)
        """, nativeQuery = true)
    int upsertIfHigher(
            @Param("playerId") Long    playerId,
            @Param("type")     String  type,
            @Param("score")    long    score,
            @Param("now")      Instant now
            );

    // 조회 (상위 N명, players와 조인해서 닉네임 사용)
    @Query("""
        SELECT r FROM PlayerRanking r JOIN FETCH r.player
        WHERE r.type = :type
        ORDER BY r.score DESC, r.achievedAt ASC, r.id ASC
        """)
    List<PlayerRanking> findTop(@Param("type") RankingType type, Pageable pageable);    // 해당 페이지의 내용들을 자동 반환

    // 조회 (자신 앞에 있는 기록 수)
    @Query("""
        SELECT COUNT(r) FROM PlayerRanking r
        WHERE r.type = :type
                AND (r.score > :score
                        OR (r.score = :score AND r.achievedAt < :at)
                        OR (r.score = :score AND r.achievedAt = :at AND r.id < :id))
        """)
    long countAhead(
            @Param("type") RankingType type,
            @Param("score") long score,
            @Param("at") Instant at,
            @Param("id") Long id
    );
}
