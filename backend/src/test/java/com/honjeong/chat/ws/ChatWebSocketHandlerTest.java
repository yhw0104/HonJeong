package com.honjeong.chat.ws;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * 연결 수립·종료·하트비트.
 */
class ChatWebSocketHandlerTest {

    private static WebSocketSession sessionOf(Long userId) {
        WebSocketSession s = mock(WebSocketSession.class);
        when(s.getAttributes()).thenReturn(Map.of(WsHandshakeInterceptor.USER_ID_KEY, userId));
        when(s.isOpen()).thenReturn(true);
        return s;
    }

    @Test
    @DisplayName("연결되면 레지스트리에 등록된다")
    void registersOnConnect() throws Exception {
        WsSessionRegistry registry = mock(WsSessionRegistry.class);
        ChatWebSocketHandler handler = new ChatWebSocketHandler(registry);
        WebSocketSession s = sessionOf(7L);

        handler.afterConnectionEstablished(s);

        verify(registry).register(eq(7L), eq(s));
    }

    @Test
    @DisplayName("끊기면 레지스트리에서 해제된다")
    void unregistersOnClose() throws Exception {
        WsSessionRegistry registry = mock(WsSessionRegistry.class);
        ChatWebSocketHandler handler = new ChatWebSocketHandler(registry);
        WebSocketSession s = sessionOf(7L);

        handler.afterConnectionClosed(s, CloseStatus.NORMAL);

        verify(registry).unregister(eq(7L), eq(s));
    }

    @Test
    @DisplayName("★ping을 받으면 pong으로 답한다 — 앱이 half-open 연결을 감지하는 수단")
    void repliesPongToPing() throws Exception {
        ChatWebSocketHandler handler = new ChatWebSocketHandler(mock(WsSessionRegistry.class));
        WebSocketSession s = sessionOf(7L);

        handler.handleMessage(s, new TextMessage("{\"type\":\"ping\"}"));

        verify(s).sendMessage(new TextMessage("{\"type\":\"pong\"}"));
    }

    @Test
    @DisplayName("모르는 프레임은 조용히 무시한다 — 클라가 보낸 것으로 서버가 죽으면 안 된다")
    void ignoresUnknownFrame() throws Exception {
        ChatWebSocketHandler handler = new ChatWebSocketHandler(mock(WsSessionRegistry.class));
        WebSocketSession s = sessionOf(7L);

        handler.handleMessage(s, new TextMessage("이건 JSON도 아니다"));

        verify(s, org.mockito.Mockito.never()).sendMessage(any());
    }
}
