package com.swordmaster.player.repository;

import com.swordmaster.player.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Long> {
    Optional<Player> findByUser_Id(Long userId);

    boolean existsByUser_Id(Long userId);

    boolean existsByNickname(String nickname);

    // 최고 기록일 때만 갱신 (엔티티를 저장하면 다른 요청이 바꾼 swordLevel을 덮어쓸 수 있으므로 컬럼만 UPDATE)
    @Modifying(flushAutomatically = true)   // 같은 트랜잭션에서 먼저 바뀐 값(재화 등)을 반영한 뒤 실행
    @Query("UPDATE Player p SET p.clearedStage = :stage WHERE p.id = :playerId AND p.clearedStage < :stage")
    int updateClearedStage(@Param("playerId") Long playerId, @Param("stage") int stage);
}
