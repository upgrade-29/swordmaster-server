package com.swordmaster.battle.controller;

import com.swordmaster.battle.dto.BattleAbandonRequest;
import com.swordmaster.battle.dto.BattleEndResponse;
import com.swordmaster.battle.dto.BattleResultVerifyRequest;
import com.swordmaster.battle.dto.BattleResultVerifyResponse;
import com.swordmaster.battle.dto.BattleSessionInfo;
import com.swordmaster.battle.dto.BattleStartRequest;
import com.swordmaster.battle.service.BattleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BattleController {
    // 본문이 없는 요청의 게임 데이터 버전 (없으면 버전이 다른 것으로 처리)
    public static final String GAME_DATA_VERSION_HEADER = "X-Game-Data-Version";

    private final BattleService battleService;

    // 진행 중인 세션이 없으면 본문 없이 200
    @GetMapping("/battle/session")
    public BattleSessionInfo getSession(
            @AuthenticationPrincipal Long userId,
            @RequestHeader(name = GAME_DATA_VERSION_HEADER, required = false) String gameDataVersion
    ) {
        return battleService.getSession(userId, gameDataVersion);
    }

    @PostMapping("/battles")
    public BattleSessionInfo start(
            @AuthenticationPrincipal Long userId,
            @RequestHeader(name = GAME_DATA_VERSION_HEADER, required = false) String gameDataVersion,
            @Valid @RequestBody BattleStartRequest request
    ) {
        return battleService.start(userId, request.stage(), gameDataVersion);
    }

    // 버전은 본문의 gameDataVersion 사용
    @PostMapping("/battle/report")
    public BattleResultVerifyResponse report(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody BattleResultVerifyRequest request
    ) {
        return battleService.report(userId, request);
    }

    @PostMapping("/battle/abandon")
    public BattleEndResponse abandon(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody BattleAbandonRequest request
    ) {
        return battleService.abandon(userId, request.battleId());
    }
}
