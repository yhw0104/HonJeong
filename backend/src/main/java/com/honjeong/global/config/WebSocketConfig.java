package com.honjeong.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.honjeong.chat.ws.ChatWebSocketHandler;
import com.honjeong.chat.ws.WsHandshakeInterceptor;

/**
 * 채팅 소켓 등록.
 *
 * <p>{@code setAllowedOrigins}를 열어 두지 않는다 — 이 소켓의 클라이언트는 네이티브 앱이라
 * Origin 헤더를 보내지 않고, 브라우저에서 붙을 일이 없다. 인증은 티켓이 담당한다
 * ({@link WsHandshakeInterceptor}).
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatWebSocketHandler handler;
    private final WsHandshakeInterceptor handshakeInterceptor;

    public WebSocketConfig(ChatWebSocketHandler handler, WsHandshakeInterceptor handshakeInterceptor) {
        this.handler = handler;
        this.handshakeInterceptor = handshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws").addInterceptors(handshakeInterceptor);
    }
}
