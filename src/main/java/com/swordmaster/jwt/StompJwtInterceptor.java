package com.swordmaster.jwt;

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

@Component
@RequiredArgsConstructor
public class StompJwtInterceptor implements ChannelInterceptor {
    private final JwtProvider jwtProvider;

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
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new AccessDeniedException("JWT가 필요합니다.");
        }
        try {
            Long userId = jwtProvider.getUserId(header.substring(7));
            accessor.setUser(new UsernamePasswordAuthenticationToken(
                    userId.toString(), null, List.of()));
        } catch (JwtException | IllegalArgumentException e) {
            throw new AccessDeniedException("JWT가 만료되었거나 유효하지 않습니다.");
        }
    }
}
