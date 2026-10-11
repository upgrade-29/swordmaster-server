package com.swordmaster.battle.repository;

import com.swordmaster.battle.entity.PlayerStageProgress;
import org.springframework.data.jpa.repository.JpaRepository;

// ID는 playerId
public interface PlayerStageProgressRepository extends JpaRepository<PlayerStageProgress, Long> {
}
