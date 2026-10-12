package com.swordmaster.player.controller;

import com.swordmaster.player.dto.PlayerResponse;
import com.swordmaster.player.service.PlayerFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class PlayerController {
    private final PlayerFacade playerFacade;

    @GetMapping("/me")
    public PlayerResponse getMe(@AuthenticationPrincipal Long userId) {
        return playerFacade.getMe(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)     // 요청 성공 시 201 Created
    public PlayerResponse create(@AuthenticationPrincipal Long userId) {
        return playerFacade.create(userId);
    }
}
