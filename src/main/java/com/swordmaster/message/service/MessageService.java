package com.swordmaster.message.service;

import com.swordmaster.common.enums.MessageType;
import com.swordmaster.message.dto.MessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final SimpMessagingTemplate messagingTemplate;

    // 특정 사용자의 구독 중인 연결들에 시스템 알림을 보낸다.
    public void sendPrivateSystemNotice(Long userId, String content) {
        MessageResponse message = new MessageResponse(
                MessageType.SYSTEM_NOTICE,
                null,
                null,
                content,
                null,
                Instant.now()
        );

        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/system",
                message
        );
    }

    // 특정 사용자의 특정 연결에만 시스템 알림을 보낸다.
    public void sendSessionSystemNotice(
            String userName,
            String sessionId,
            String content) {

        MessageResponse message = new MessageResponse(
                MessageType.SYSTEM_NOTICE,
                null,
                null,
                content,
                null,
                Instant.now()
        );

        SimpMessageHeaderAccessor headers =
                SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headers.setSessionId(sessionId);
        headers.setLeaveMutable(true);

        messagingTemplate.convertAndSendToUser(
                userName,
                "/queue/system",
                message,
                headers.getMessageHeaders()
        );
    }

    public void sendSystemNotice(String content) {
        MessageResponse message = new MessageResponse(
                MessageType.SYSTEM_NOTICE, null, null,
                content, null, Instant.now());
        messagingTemplate.convertAndSend("/topic/notices", message);
    }

    public void sendEnhancementNotice(Long userId, String nickname, int level) {
        MessageResponse message = new MessageResponse(
                MessageType.SWORD_ENHANCEMENT, userId, nickname,
                nickname + "님이 검 " + level + "강에 성공했습니다!",
                level, Instant.now());
        messagingTemplate.convertAndSend("/topic/notices", message);
    }

    // 호출하는 서비스에서 친구 관계·차단 여부와 발신자 정보를 검증해야 한다.
    public void deliverPrivateMessage(Long senderId, String senderNickname,
                                      Long receiverId, String content) {
        MessageResponse message = new MessageResponse(
                MessageType.PRIVATE_CHAT, senderId, senderNickname,
                content, null, Instant.now());
        messagingTemplate.convertAndSendToUser(
                receiverId.toString(), "/queue/messages", message);
    }

    // 인증 정보로 확인한 발신자의 메시지를 전체 채팅 구독자에게 전달한다.
    public void deliverPublicMessage(
            Long senderId,
            String senderNickname,
            String content) {

        MessageResponse message = new MessageResponse(
                MessageType.PUBLIC_CHAT,
                senderId,
                senderNickname,
                content,
                null,
                Instant.now()
        );

        messagingTemplate.convertAndSend("/topic/chat/public", message);
    }
}
