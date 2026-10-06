package com.swordmaster.user.service;

import com.swordmaster.common.BusinessException;
import com.swordmaster.jwt.JwtProvider;
import com.swordmaster.sword.entity.Sword;
import com.swordmaster.sword.repository.SwordRepository;
import com.swordmaster.user.dto.LoginRequest;
import com.swordmaster.user.dto.LoginResponse;
import com.swordmaster.user.dto.SignupRequest;
import com.swordmaster.user.dto.UserResponse;
import com.swordmaster.user.entity.User;
import com.swordmaster.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final SwordRepository swordRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public void signup(@Valid SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "이미 계정을 생성한 이메일입니다.");
        }//TODO : 이메일로 아이디 찾기만들기도 재밌어보이네

        String encodedPassword = //유출 방지(불필요하지만 멋졌죠?)
                passwordEncoder.encode(request.getPassword());

        User user = new User(
                request.getEmail(),
                encodedPassword,
                request.getNickname()
        );//TODO : 계정 생성 시 기본 칼과 아티펙트를 지급
        Sword sword = new Sword(user);

        userRepository.save(user);
        swordRepository.save(sword);
    }

    @Transactional
    public LoginResponse login(@Valid LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(
                    HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        String token = jwtProvider.createToken(user.getId());
        user.updateLastLoginAt();
        return new LoginResponse(token);
    }

    @Transactional
    public UserResponse getMe(Long userId) {
        User user = userRepository.findById(userId).
                orElseThrow( ()->
                        new BusinessException("getMe 오류 : 사용자 없음")
                );
        Sword sword = swordRepository.findById(userId).
                orElseThrow( ()->
                        new BusinessException("getMe 오류 : sword 없음")
                );

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getGold(),
                user.getDiamond(),
                sword.getLevel()
        );
    }

    //TODO 나중에 연결 종료 시 게임 데이터를 저장해서 거기부터 다시하기를 원하는 경우 구현2
    @Transactional
    public UserResponse getGameData(Long userId) {

        return null;
    }
}
