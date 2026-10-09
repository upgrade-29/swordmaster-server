package com.swordmaster.equipment.artifact.repository;

import com.swordmaster.player.entity.Player;
import com.swordmaster.equipment.artifact.entity.PlayerArtifact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArtifactRepository extends JpaRepository<PlayerArtifact, Long> {
    // 플레이어의 인벤토리 전체 반환 (없으면 빈 리스트 반환)
    List<PlayerArtifact> findAllByPlayer(Player player);

    // 유저 정보까지 조회한 후 일치한 아티팩트 반환
    Optional<PlayerArtifact> findByPlayerAndCode(Player player, String code);
//
//    // 해당 유저의 특정 슬롯에 장착된 아티팩트 반환
//    Optional<PlayerArtifact> findByPlayer_User_IdAndEquipSlot(Long userId, Integer equipSlot);
}
