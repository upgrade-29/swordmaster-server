package com.swordmaster.user.service;

import com.swordmaster.common.BusinessException;
import com.swordmaster.common.enums.LoginProvider;
import com.swordmaster.jwt.JwtProvider;
import com.swordmaster.jwt.service.RefreshTokenService;
import com.swordmaster.user.dto.LoginRequest;
import com.swordmaster.user.dto.LoginResponse;
import com.swordmaster.user.dto.SignupRequest;
import com.swordmaster.user.dto.UserResponse;
import com.swordmaster.user.entity.User;
import com.swordmaster.user.entity.UserIdentity;
import com.swordmaster.user.repository.UserIdentityRepository;
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
    private final UserIdentityRepository userIdentityRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public void signup(@Valid SignupRequest request) {
        String localEmail = request.getEmail();
        boolean exists = userIdentityRepository.existsByProviderAndProviderUserId(
                LoginProvider.LOCAL,
                localEmail
        ); //로그인 방식은 다른 api를 사용하거나 우리가 만든 local뿐이기 때문에 우리가 검증가능한 것은 local뿐이다.

        if (exists) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "이미 계정을 생성한 이메일입니다.");
        }

        String encodedPassword = //유출 방지(불필요하지만 멋졌죠?)
                passwordEncoder.encode(request.getPassword());

        User user = new User(
                request.getEmail(),
                encodedPassword//useridentity가 생겨서 불필요한 부분이지만 제거하면 오류가 발생할까봐 남겨둠
        );

        UserIdentity identity = UserIdentity.createLocal(
                user,
                localEmail,
                encodedPassword
        );

        userRepository.save(user);
        userIdentityRepository.saveAndFlush(identity);//위와 달리 저장 값을 즉시 db에 적용하는 방법
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

        return issueTokens(user);
    }

    @Transactional
    public LoginResponse googleLogin(
            String verifiedGoogleSub,
            String verifiedEmail
    ) {
        UserIdentity identity = userIdentityRepository
                .findByProviderAndProviderUserId(
                        LoginProvider.GOOGLE,
                        verifiedGoogleSub
                )
                .orElseGet(() -> createGoogleIdentity(
                        verifiedGoogleSub,
                        verifiedEmail
                ));

        return issueTokens(identity.getUser());
    }

    private UserIdentity createGoogleIdentity(
            String googleSub,
            String email
    ) {
        User user = userRepository.save(new User(email, "GoogleLogin"));

        UserIdentity identity = UserIdentity.createGoogle(
                user,
                googleSub,
                email
        );

        return userIdentityRepository.saveAndFlush(identity);
    }

    private LoginResponse issueTokens(User user) {
        String accessToken = jwtProvider.createToken(user.getId());
        String refreshToken = refreshTokenService.issue(user);
        user.updateLastLoginAt();

        return new LoginResponse(accessToken, refreshToken);
    }

    @Transactional
    public UserResponse getMe(Long userId) {
        User user = userRepository.findById(userId).
                orElseThrow(()->
                        new BusinessException(HttpStatus.NOT_FOUND, "getMe 오류 : 사용자 없음")
                );

        return new UserResponse(
                user.getId(),
                user.getEmail()
        );
    }
}
