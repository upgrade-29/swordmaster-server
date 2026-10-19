package com.swordmaster.jwt;

import com.swordmaster.user.entity.User;
import com.swordmaster.user.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {//admin의 인증을 만들어주는 기능
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    // 토큰이 필요 없는 인증 API는 검사 제외(만료된 access 토큰이 헤더에 남아있어도 refresh 가능하도록)
    private static final Set<String> SKIP_PATHS = Set.of(
            "/api/auth/signup",
            "/api/auth/login",
            "/api/auth/refresh"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return SKIP_PATHS.contains(request.getServletPath());
    }

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

                User user = userRepository.findById(userId).orElse(null);

                if (user == null || user.getRole() == null) {
                    SecurityContextHolder.clearContext();
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }

                SimpleGrantedAuthority authority =
                        new SimpleGrantedAuthority(
                                "ROLE_" + user.getRole().name()//admin의 인증을 부여함, 이후 filter를 통해서 spring이 자체적으로 인증을 해준다.
                        );

                //Authentication 생성
                Authentication authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                List.of(authority)
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
