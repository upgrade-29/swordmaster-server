package com.swordmaster.user.controller;

import com.swordmaster.externalapi.service.ExternalAPIService;
import com.swordmaster.jwt.service.RefreshTokenService;
import com.swordmaster.user.dto.*;
import com.swordmaster.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final ExternalAPIService externalAPIService;

    @PostMapping("/api/auth/signup")
    public ResponseEntity<Void> signup(
            @Valid @RequestBody SignupRequest request){
        userService.signup(request);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request){
        return ResponseEntity.ok(userService.login(request));
    }

    @PostMapping("/api/auth/login/guest")
    public ResponseEntity<LoginResponse> guestLogin(){
        return ResponseEntity.ok(userService.guestlogin());
    }

    @PostMapping("/api/auth/refresh") //자동로그인, access 토큰 만료 시 재발급
    public ResponseEntity<LoginResponse> refresh(
            @Valid @RequestBody RefreshRequest request){
        return ResponseEntity.ok(refreshTokenService.refresh(request.getRefreshToken()));
    }

    @PostMapping("/api/auth/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal Long userId){
        refreshTokenService.revoke(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/user/me")
    public ResponseEntity<UserResponse> getMe(
            @AuthenticationPrincipal Long userId //jwt에 존재하는 userId값을 사용
    ){
        return ResponseEntity.ok(userService.getMe(userId));
    }

    @PostMapping("/api/user/identities/google")
    public ResponseEntity<IdentityLinkResponse> linkGoogle(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody GoogleLoginRequest request
    ) {
        return ResponseEntity.ok(externalAPIService.linkGoogle(userId, request));
    }
}
