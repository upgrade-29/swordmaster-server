package com.swordmaster.jwt.controller;

import com.swordmaster.jwt.dto.RefreshTokenRequest;
import com.swordmaster.jwt.service.RedisTokenService;
import com.swordmaster.user.dto.LoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class TokenController {

    private final RedisTokenService tokenService;

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(tokenService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        tokenService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
