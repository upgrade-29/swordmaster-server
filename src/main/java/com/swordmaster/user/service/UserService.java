package com.swordmaster.user.service;

import com.swordmaster.common.BusinessException;
import com.swordmaster.jwt.JwtProvider;
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

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public void signup(@Valid SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "이미 계정을 생성한 이메일입니다.");
        }//TODO : 이메일로 아이디 찾기만들기도 재밌어보이네

        String encodedPassword = //유출 방지(불필요하지만 멋졌죠?)
                passwordEncoder.encode(request.getPassword());

        User user = new User(
                request.getEmail(),
                encodedPassword,
                request.getNickname()
        );

        userRepository.save(user);
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
                orElseThrow(()->
                        new BusinessException(HttpStatus.NOT_FOUND, "getMe 오류 : 사용자 없음")
                );

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname()
        );
    }
}
