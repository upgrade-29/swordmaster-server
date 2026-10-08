package com.swordmaster.config;

import com.swordmaster.jwt.StompJwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker //spring이 stomp방식을 대신 처리해주는 어노테이션
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final StompJwtInterceptor stompJwtInterceptor; //모든 인바운드 요청은 stompJwtInterceptor를 거친다.

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws");
    }
    // "/ws"로 클라이언트는 웹소켓 연결이 가능하다. 그리고 어떤 작업인지는 stomp 방식를 통해서 결정(구독)하겠다.

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setUserDestinationPrefix("/user"); // /user 로 시작하는 주소를 사용자용(개인) 전달 주소로 처리 후 브로커에게 전달
        registry.enableSimpleBroker("/topic", "/queue"); // /topic(공용), /queue(개인) 로 시작하는 주소를 브로커로 만들어서 구독을 하거나 문자를 보낼 수 있다.
        registry.setApplicationDestinationPrefixes("/app"); // /app 으로 시작하는 주소를 보내면 서버가 뒤를 처리 메서드로 연결한다. (해당 방식들은 전부 @MessageMapping 이라는 http가 아닌 방식을 사용한다. 무조건 ws를 통해서 app으로 호출되어야하는 것이다.)
}

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) { //대부분 인바운드(클라->서버)라고 배웠다
        registration.interceptors(stompJwtInterceptor);
    } //jwt는 http 요청을 확인하기 때문에(그리고 ws는 전부 통과하도록 했기 때문에) ws용 jwt라고 생각하면 편하다.
}
