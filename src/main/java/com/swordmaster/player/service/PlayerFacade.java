package com.swordmaster.player.service;

import com.swordmaster.currency.dto.CurrenciesResponse;
import com.swordmaster.currency.service.CurrencyService;
import com.swordmaster.equipment.artifact.dto.ArtifactResponse;
import com.swordmaster.equipment.artifact.service.ArtifactService;
import com.swordmaster.equipment.sword.dto.SwordResponse;
import com.swordmaster.player.dto.PlayerResponse;
import com.swordmaster.player.entity.Player;
import com.swordmaster.user.dto.UserResponse;
import com.swordmaster.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 플레이어와 여러 다른 기능(아티팩트, 재화 등)을 함께 다루는 작업을 묶는 서비스
@Service
@RequiredArgsConstructor
public class PlayerFacade {
    private final PlayerService   playerService;
    private final UserService     userService;
    private final CurrencyService currencyService;
    private final ArtifactService artifactService;

    // 조회
    @Transactional(readOnly = true)
    public PlayerResponse getMe(Long userId) {
        UserResponse           userResponse       = userService.getMe(userId);
        Player                 player             = playerService.getMe(userId);
        SwordResponse          swordResponse      = SwordResponse.from(player);
        CurrenciesResponse     currenciesResponse = currencyService.getAll(player);
        List<ArtifactResponse> artifactResponses  = artifactService.getAll(player);

        return PlayerResponse.of(
                userResponse,
                swordResponse,
                currenciesResponse,
                artifactResponses);
    }

    // 생성
    @Transactional
    public PlayerResponse create(Long userId) {
        UserResponse userResponse = userService.getMe(userId);
        Player       player       = playerService.create(userId);

        // 초기 검 생성
        SwordResponse swordResponse = SwordResponse.from(player);

        // 초기 재화 생성
        CurrenciesResponse currenciesResponse = currencyService.createAll(player);

        // 초기 아티팩트 생성
        List<ArtifactResponse> artifactResponses = artifactService.createDefault(player);

        return PlayerResponse.of(
                userResponse,
                swordResponse,
                currenciesResponse,
                artifactResponses);
    }
}
