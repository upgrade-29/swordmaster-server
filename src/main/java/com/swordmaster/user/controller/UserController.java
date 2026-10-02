package com.swordmaster.user.controller;

import com.swordmaster.user.dto.LoginRequest;
import com.swordmaster.user.dto.LoginResponse;
import com.swordmaster.user.dto.SignupRequest;
import com.swordmaster.user.dto.UserResponse;
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

    @GetMapping("/api/user/me")
    public ResponseEntity<UserResponse> getMe(
            @AuthenticationPrincipal Long userId //jwt에 존재하는 userId값을 사용
    ){
        return ResponseEntity.ok(userService.getMe(userId));
    }

    //TODO 나중에 연결 종료 시 게임 데이터를 저장해서 거기부터 다시하기를 원하는 경우 구현
    @GetMapping("/api/game-data")
    public ResponseEntity<UserResponse> getGameData(
            @AuthenticationPrincipal Long userId
    ){
        return ResponseEntity.ok(null
                //userService.getGameData(userId)
        );
    }
}
