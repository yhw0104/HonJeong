package com.honjeong.chat.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

/**
 * 채팅 소켓의 연결 수명과 하트비트를 다룬다.
 *
 * <p>클라이언트가 이 소켓으로 보낼 수 있는 것은 {@code ping} 하나뿐이다 —
 * <b>메시지 전송은 REST</b>({@code POST /api/conversations/{id}/messages})가 담당한다.
 * 소켓을 수신 전용으로 두면 인가 표면이 줄고, 소켓이 끊겨도 전송은 계속 된다.
 *
 * <p>하트비트를 애플리케이션 레벨 JSON으로 하는 이유: WebSocket 프로토콜에 ping/pong 프레임이
 * 있지만 <b>자바스크립트에서는 접근할 수 없다</b>(브라우저·RN 모두 JS API로 노출하지 않는다).
 */
@Component
public class ChatWebSocketHandler extends AbstractWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private static final String PING = "\"type\":\"ping\"";
    private static final TextMessage PONG = new TextMessage("{\"type\":\"pong\"}");

    private final WsSessionRegistry registry;

    public ChatWebSocketHandler(WsSessionRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        registry.register(userIdOf(session), session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        registry.unregister(userIdOf(session), session);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        if (!(message instanceof TextMessage text) || !text.getPayload().contains(PING)) {
            // 모르는 프레임은 무시한다. 클라이언트가 보낸 것으로 서버가 죽어서는 안 된다.
            return;
        }
        session.sendMessage(PONG);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.debug("[ws] 전송 오류 session={}", session.getId(), exception);
    }

    /**
     * 세션의 주인. 핸드셰이크를 통과한 세션에는 반드시 들어 있다
     * ({@link WsHandshakeInterceptor}가 없으면 false를 돌려 연결을 막는다).
     */
    private static Long userIdOf(WebSocketSession session) {
        return (Long) session.getAttributes().get(WsHandshakeInterceptor.USER_ID_KEY);
    }
}
