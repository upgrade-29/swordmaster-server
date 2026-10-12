package com.swordmaster.battle.repository;

import com.swordmaster.battle.entity.BattleHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BattleHistoryRepository extends JpaRepository<BattleHistory, Long> {
    // 이미 정산한 전투인지 확인
    boolean existsByBattleId(String battleId);
}
