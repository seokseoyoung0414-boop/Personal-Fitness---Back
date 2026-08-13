package com.nody.nodyspring.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 연결 주소 (프론트에서 연결할 때 사용됨)
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*")  // 모든 도메인 허용
                .withSockJS(); // SockJS fallback 지원
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 클라이언트에서 받을 때 prefix
        registry.enableSimpleBroker("/topic");
        // 서버로 보낼 때 prefix
        registry.setApplicationDestinationPrefixes("/app");
    }
}