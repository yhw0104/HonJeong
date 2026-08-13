package com.honjeong.chat.ws;

import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * 핸드셰이크에서 티켓을 소모해 연결의 신원을 확정한다.
 *
 * <p>★ 핸드셰이크는 {@code SecurityFilterChain}을 타지 않는다. 그래서 이 인터셉터가
 * <b>유일한 관문</b>이다 — 여기서 false를 돌려주면 연결이 아예 성립하지 않는다.
 *
 * <p>티켓 발급({@code POST /api/ws-ticket})은 일반 REST라 인증·탈퇴·정지 판정을 이미 통과한
 * 상태다. 따라서 여기서는 "이 티켓이 유효한가"만 보면 되고, 그 판정을 한 벌 더 구현하지 않는다.
 */
@Component
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    /** 세션 attributes에 userId를 담는 키. 핸들러가 이 키로 꺼낸다. */
    public static final String USER_ID_KEY = "userId";

    private final WsTicketService wsTicketService;

    public WsHandshakeInterceptor(WsTicketService wsTicketService) {
        this.wsTicketService = wsTicketService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Map<String, Object> attributes) {
        Long userId = wsTicketService.consume(ticketOf(request));
        if (userId == null) {
            return false;
        }
        attributes.put(USER_ID_KEY, userId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Exception exception) {
        // 할 일 없음.
    }

    private static String ticketOf(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servlet) {
            return servlet.getServletRequest().getParameter("ticket");
        }
        return null;
    }
}
