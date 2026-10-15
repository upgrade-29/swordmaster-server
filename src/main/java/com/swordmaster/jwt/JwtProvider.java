package com.swordmaster.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

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
    public String createToken(Long userId, String sessionId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId.toString()) // sub: 사용자 ID
                .claim("sid", sessionId)
                .issuedAt(now)              // iat: 발급 시각
                .expiration(expiration)    // exp: 만료 시각
                .signWith(secretKey)       // 키로 서명
                .compact();                // JWT 문자열 생성
    }

    public record AccessIdentity(Long userId, String sessionId) {}

    // 서명과 만료 시각을 검증하고 사용자와 로그인 세션을 추출한다.
    public AccessIdentity getIdentity(String token) {
        Claims claims = parseClaims(token);
        if (claims.getExpiration() == null) {
            throw new IllegalArgumentException("만료 시각이 필요합니다.");
        }
        Long userId = Long.valueOf(claims.getSubject());
        String sessionId = claims.get("sid", String.class);
        if (sessionId == null) {
            throw new IllegalArgumentException("로그인 세션이 필요합니다.");
        }
        String normalized = UUID.fromString(sessionId).toString();
        if (!normalized.equals(sessionId)) {
            throw new IllegalArgumentException("잘못된 로그인 세션입니다.");
        }
        return new AccessIdentity(userId, sessionId);
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
