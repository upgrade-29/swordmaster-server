package com.swordmaster.externalapi.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.swordmaster.common.BusinessException;
import com.swordmaster.user.dto.GoogleLoginRequest;
import com.swordmaster.user.dto.LoginResponse;
import com.swordmaster.user.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@Service
public class ExternalAPIService {

    private final UserService userService;
    private final GoogleIdTokenVerifier googleVerifier;

    public ExternalAPIService(
            UserService userService,
            @Value("${GOOGLE_WEB_CLIENT_ID}") String clientId
    ) {
        if (clientId.isBlank()) {
            throw new IllegalStateException(
                    "GOOGLE_WEB_CLIENT_ID 설정이 필요합니다."
            );
        }

        this.userService = userService;

        this.googleVerifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance()
        )
                .setAudience(List.of(clientId))
                .build();
    }

    public LoginResponse googleLogin(GoogleLoginRequest request) {
        // Google 검증은 DB 트랜잭션을 시작하기 전에 수행한다.
        GoogleIdToken.Payload payload = verifyGoogle(request.idToken());

        String googleSub = payload.getSubject();

        // 이메일은 부가 정보이며 로그인 식별자가 아니다.
        String email = Boolean.TRUE.equals(payload.getEmailVerified())
                ? payload.getEmail()
                : null;

        if (email != null
                && (email.isBlank() || email.length() > 254)) {
            email = null;
        }

        // 별도 Spring 서비스 호출부터 DB 트랜잭션이 시작된다.
        return userService.googleLogin(googleSub, email);
    }

    private GoogleIdToken.Payload verifyGoogle(String idToken) {
        try {
            GoogleIdToken verifiedToken = googleVerifier.verify(idToken);

            if (verifiedToken == null) {
                throw invalidGoogleToken();
            }

            GoogleIdToken.Payload payload = verifiedToken.getPayload();
            String subject = payload.getSubject();

            if (subject == null
                    || subject.isBlank()
                    || subject.length() > 255) {
                throw invalidGoogleToken();
            }

            return payload;

        } catch (IOException e) {
            throw new BusinessException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "구글 인증 서버와 통신할 수 없습니다."
            );
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw invalidGoogleToken();
        }
    }

    private BusinessException invalidGoogleToken() {
        return new BusinessException(
                HttpStatus.UNAUTHORIZED,
                "유효하지 않은 구글 ID 토큰입니다."
        );
    }
}