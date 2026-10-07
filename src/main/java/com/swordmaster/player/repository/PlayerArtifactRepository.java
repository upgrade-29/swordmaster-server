package com.swordmaster.player.repository;

import com.swordmaster.player.entity.PlayerArtifact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlayerArtifactRepository extends JpaRepository<PlayerArtifact, Long> {
    // 플레이어의 인벤토리 전체 반환 (없으면 빈 리스트 반환)
    List<PlayerArtifact> findAllByPlayer_User_Id(Long userId);

    // 유저 정보까지 조회한 후 일치한 아티팩트 반환 (타 유저의 아이템 조작 방지)
    Optional<PlayerArtifact> findByIdAndPlayer_User_Id(Long id, Long userId);

    // 해당 유저의 특정 슬롯에 장착된 아티팩트 반환
    Optional<PlayerArtifact> findByPlayer_User_IdAndEquipSlot(Long userId, Integer equipSlot);
}
