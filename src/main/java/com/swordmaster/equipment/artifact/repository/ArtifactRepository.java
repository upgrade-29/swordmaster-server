package com.swordmaster.equipment.artifact.repository;

import com.swordmaster.player.entity.Player;
import com.swordmaster.equipment.artifact.entity.PlayerArtifact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.parameters.P;

import java.util.List;
import java.util.Optional;

public interface ArtifactRepository extends JpaRepository<PlayerArtifact, Long> {
    // 플레이어의 인벤토리 전체 반환 (없으면 빈 리스트 반환)
    List<PlayerArtifact> findAllByPlayer(Player player);

    // 플레이어의 특정 아티팩트 반환
    Optional<PlayerArtifact> findByPlayerAndCode(Player player, String code);

    // 장착된 특정 아티팩트(또는 리스트) 반환
    Optional<PlayerArtifact> findByPlayerAndCodeAndEquipSlot(Player player, String code, int equipSlot);

    List<PlayerArtifact> findAllByPlayerAndEquipSlotIsNotNull(Player player);
}
