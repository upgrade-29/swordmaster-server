package com.swordmaster.jwt;

import com.swordmaster.jwt.service.RedisTokenService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class StompJwtInterceptor implements ChannelInterceptor {
    private final JwtProvider jwtProvider;
    private final RedisTokenService redisTokenService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (command == StompCommand.CONNECT) {
            authenticate(accessor);
            return message;
        }
        if (accessor.getUser() == null) {
            throw new AccessDeniedException("인증이 필요합니다.");
        }
        if (command == StompCommand.SUBSCRIBE) { //TODO : 현재 사용하는 채팅창의 종류인데, 친구마다 개인 채팅이 있는 경우는 어떡하는가?
            String destination = accessor.getDestination();
            boolean allowed = "/topic/notices".equals(destination)
                    || "/topic/chat/public".equals(destination)
                    || "/user/queue/messages".equals(destination)
                    || "/user/queue/system".equals(destination)
                    || "/user/queue/errors".equals(destination);
            if (!allowed) {
                throw new AccessDeniedException("허용되지 않은 구독 주소입니다.");
            }
        }
        // Client 송신은 전체 채팅 처리 주소만 허용한다.(ws stomp 방식만을 허용)
        if (command == StompCommand.SEND) {
            if (!"/app/chat/public".equals(accessor.getDestination())) {
                throw new AccessDeniedException("허용되지 않은 송신 주소입니다.");
            }
        }
        if (command == StompCommand.SEND || command == StompCommand.SUBSCRIBE) {
            checkSession(accessor, command == StompCommand.SEND);
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new AccessDeniedException("JWT가 필요합니다.");
        }
        try {
            String token = header.substring(7);
            JwtProvider.AccessIdentity identity = jwtProvider.getIdentity(token);
            if (!redisTokenService.isActive(identity.userId(), identity.sessionId())) {
                throw new AccessDeniedException("로그인이 만료되었습니다.");
            }
            Map<String, Object> attributes = accessor.getSessionAttributes();
            if (attributes == null) {
                throw new AccessDeniedException("연결 정보가 없습니다.");
            }
            attributes.put("accessToken", token);
            accessor.setUser(new UsernamePasswordAuthenticationToken(
                    identity.userId().toString(), null, List.of()));
        } catch (JwtException | IllegalArgumentException e) {
            throw new AccessDeniedException("JWT가 만료되었거나 유효하지 않습니다.");
        }
    }

    private void checkSession(StompHeaderAccessor accessor, boolean activity) {
        Map<String, Object> attributes = accessor.getSessionAttributes();
        Object value = attributes == null ? null : attributes.get("accessToken");
        if (!(value instanceof String token)) {
            throw new AccessDeniedException("인증이 필요합니다.");
        }
        try {
            JwtProvider.AccessIdentity identity = jwtProvider.getIdentity(token);
            boolean active = activity
                    ? redisTokenService.touch(identity.userId(), identity.sessionId())
                    : redisTokenService.isActive(identity.userId(), identity.sessionId());
            if (!active) {
                throw new AccessDeniedException("로그인이 만료되었습니다.");
            }
        } catch (JwtException | IllegalArgumentException e) {
            throw new AccessDeniedException("새 access token으로 다시 연결해주세요.");
        }
    }
}
