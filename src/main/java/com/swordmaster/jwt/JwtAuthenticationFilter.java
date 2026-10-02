package com.swordmaster.jwt;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        //jwt(Authorization 헤더) 가져옮
        String header = request.getHeader("Authorization");

        if (header != null &&
                header.startsWith("Bearer ")) {
            //jwt가 없는 경우가 아니면 배운 것 처럼 헤더의 불필요한 bearer부분 제거(토큰 값만 받기)
            String token = header.substring(7);

            try { // 검증과 사용자 ID 추출을 한 번에 수행
                Long userId =
                        jwtProvider.getUserId(token);

                //Authentication 생성
                Authentication authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                List.of()
                        );

                // 현재 요청의 SecurityContext에 인증 정보 저장
                SecurityContextHolder
                        .getContext()
                        .setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }

        //인증 정보 전달
        filterChain.doFilter(request, response);
    }
}
