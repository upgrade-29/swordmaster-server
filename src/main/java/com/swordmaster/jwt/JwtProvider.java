package com.swordmaster.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtProvider {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs
    ) {
        // Base64 문자열을 서명·검증에 사용할 키로 변환
        this.secretKey =
                Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
    }

    // 로그인 성공 후 사용자 ID로 JWT 생성
    public String createToken(Long userId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId.toString()) // sub: 사용자 ID
                .issuedAt(now)              // iat: 발급 시각
                .expiration(expiration)    // exp: 만료 시각
                .signWith(secretKey)       // 키로 서명
                .compact();                // JWT 문자열 생성
    }

    // 서명, 만료 여부, 필요한 사용자 정보 검증
    public boolean validateToken(String token) {
        try {
            Claims claims = parseClaims(token);

            // 이 서비스의 토큰은 만료 시각이 반드시 있어야 함
            if (claims.getExpiration() == null) {
                return false;
            }

            // 사용자 ID가 Long으로 변환 가능한지도 확인
            Long.parseLong(claims.getSubject());

            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // 검증한 JWT의 sub에서 사용자 ID 추출
    public Long getUserId(String token) {
        Claims claims = parseClaims(token);
        if (claims.getExpiration() == null) {
            throw new IllegalArgumentException("만료 시각이 필요합니다.");
        }
        return Long.valueOf(claims.getSubject());
    }

    // 서명을 검증하고 토큰에 담긴 정보(Claims)를 반환
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
