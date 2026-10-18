package com.swordmaster.jwt.service;

import com.swordmaster.common.BusinessException;
import com.swordmaster.jwt.JwtProvider;
import com.swordmaster.jwt.entity.RefreshToken;
import com.swordmaster.jwt.repository.RefreshTokenRepository;
import com.swordmaster.user.dto.LoginResponse;
import com.swordmaster.user.entity.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;

    // refresh 토큰 발급 : 기존 행이 있으면 교체(이전 기기 로그아웃), 없으면 새로 생성
    // 반환값은 클라이언트에 전달할 원문 토큰(DB에는 해시만 저장)
    @Transactional
    public String issue(User user) {
        String rawToken = jwtProvider.createRefreshToken();
        String tokenHash = jwtProvider.hashToken(rawToken);
        Instant expiresAt = jwtProvider.getRefreshExpiresAt();

        refreshTokenRepository.findByUserId(user.getId())
                .ifPresentOrElse(
                        token -> token.rotate(tokenHash, expiresAt),
                        () -> refreshTokenRepository.save(
                                new RefreshToken(user, tokenHash, expiresAt))
                );

        return rawToken;
    }

    // 자동로그인 : refresh 토큰 검증 후 access/refresh 토큰 재발급(rotation)
    @Transactional
    public LoginResponse refresh(String rawToken) {
        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHash(jwtProvider.hashToken(rawToken))
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.UNAUTHORIZED, "유효하지 않은 refresh 토큰입니다.")); //다른 기기 로그인, 로그아웃, 이미 사용된 토큰 포함

        if (refreshToken.isExpired()) { //만료된 행은 다음 로그인 시 issue()에서 교체됨
            throw new BusinessException(
                    HttpStatus.UNAUTHORIZED, "만료된 refresh 토큰입니다.");
        }

        User user = refreshToken.getUser();
        String newRawToken = jwtProvider.createRefreshToken();
        refreshToken.rotate(jwtProvider.hashToken(newRawToken), jwtProvider.getRefreshExpiresAt()); //기존 토큰은 재사용 불가
        user.updateLastLoginAt();

        return new LoginResponse(jwtProvider.createToken(user.getId()), newRawToken);
    }

    // 로그아웃 : 해당 유저의 refresh 토큰 삭제
    @Transactional
    public void revoke(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}
