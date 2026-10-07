package com.swordmaster.message.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class ConnectionNoticeScheduler {

    private static final Duration NOTICE_INTERVAL =
            Duration.ofHours(1);

    private final MessageService messageService;

    private final Map<String, ConnectionState> connections =
            new HashMap<>();

    @EventListener
    public synchronized void onConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor =
                StompHeaderAccessor.wrap(event.getMessage());

        String sessionId = accessor.getSessionId();

        if (sessionId == null || event.getUser() == null) {
            return;
        }

        Instant connectedAt = Instant.now();

        connections.putIfAbsent(
                sessionId,
                new ConnectionState(
                        event.getUser().getName(),
                        connectedAt,
                        connectedAt.plus(NOTICE_INTERVAL)
                )
        );
    }

    @EventListener
    public synchronized void onDisconnected(
            SessionDisconnectEvent event) {

        connections.remove(event.getSessionId());
    }

    @Scheduled(fixedDelay = 1000)
    public synchronized void sendDueNotices() {
        Instant now = Instant.now();

        for (var entry : connections.entrySet()) {
            ConnectionState state = entry.getValue();

            if (now.isBefore(state.nextNoticeAt())) {
                continue;
            }

            long elapsedHours =
                    Duration.between(state.connectedAt(), now).toHours();

            String content =
                    "접속 후 " + elapsedHours
                            + "시간이 지났습니다. 잠시 쉬어가세요.";

            try {
                messageService.sendSessionSystemNotice(
                        state.userName(),
                        entry.getKey(),
                        content
                );

                // 접속 시각을 기준으로 다음 정시 간격을 계산한다.
                Instant nextNoticeAt = state.connectedAt().plus(
                        NOTICE_INTERVAL.multipliedBy(elapsedHours + 1)
                );

                entry.setValue(new ConnectionState(
                        state.userName(),
                        state.connectedAt(),
                        nextNoticeAt
                ));
            } catch (RuntimeException exception) {
                log.warn(
                        "접속 시간 알림 전송 실패: sessionId={}",
                        entry.getKey(),
                        exception
                );

                // 전송 실패 시 1분 뒤 재시도한다.
                entry.setValue(new ConnectionState(
                        state.userName(),
                        state.connectedAt(),
                        now.plusSeconds(60)
                ));
            }
        }
    }

    private record ConnectionState(
            String userName,
            Instant connectedAt,
            Instant nextNoticeAt
    ) {}
}
