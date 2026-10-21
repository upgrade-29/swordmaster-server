package com.swordmaster.externalapi.controller;

import com.swordmaster.equipment.sword.dto.SwordEnhanceResponse;
import com.swordmaster.equipment.sword.dto.SwordRequest;
import com.swordmaster.externalapi.service.ExternalAPIService;
import com.swordmaster.user.dto.GoogleLoginRequest;
import com.swordmaster.user.dto.LoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class ExternalAPIController {
    private final ExternalAPIService externalAPIService;
//    private String googleGamesPostApi = "https://oauth2.googleapis.com/token";
//    private String googleGamesGetApi = "https://games.googleapis.com/games/v1/players/me";

    //TODO 이후에 모든 로그인 방법을 추가한다 (kako, naver, ...)
    @PostMapping("/login/google")
    public ResponseEntity<LoginResponse> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request
    ) {
        return ResponseEntity.ok(externalAPIService.googleLogin(request));
    }

//    @PostMapping("/googleGames")
//    public void postAPI(){    }
//
//    @GetMapping("/googleGames")
//    public void getAPI() {
//    }
}
