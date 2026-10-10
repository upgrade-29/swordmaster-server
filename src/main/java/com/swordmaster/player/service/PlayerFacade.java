package com.swordmaster.player.service;

import com.swordmaster.currency.CurrencyType;
import com.swordmaster.currency.service.CurrencyService;
import com.swordmaster.equipment.artifact.dto.ArtifactResponse;
import com.swordmaster.equipment.artifact.service.ArtifactService;
import com.swordmaster.player.dto.PlayerResponse;
import com.swordmaster.player.entity.Player;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

// 플레이어와 여러 다른 기능(아티팩트, 재화 등)을 함께 다루는 작업을 묶는 서비스
@Service
@RequiredArgsConstructor
public class PlayerFacade {
    private final PlayerService   playerService;
    private final CurrencyService currencyService;
    private final ArtifactService artifactService;

    // 조회
    @Transactional(readOnly = true)
    public PlayerResponse getMe(Long userId) {
        Player                  player     = playerService.getMe(userId);
        Map<CurrencyType, Long> currencies = currencyService.getAll(player);
        List<ArtifactResponse>  artifacts  = artifactService.getAll(player);

        return PlayerResponse.of(player, currencies, artifacts);
    }

    // 생성
    @Transactional
    public PlayerResponse create(Long userId) {
        Player player = playerService.create(userId);

        // 초기 재화 생성
        Map<CurrencyType, Long> currencies = currencyService.createAll(player);

        // 초기 아티팩트 생성
        List<ArtifactResponse> artifacts = artifactService.createDefault(player);

        return PlayerResponse.of(player, currencies, artifacts);
    }
}
