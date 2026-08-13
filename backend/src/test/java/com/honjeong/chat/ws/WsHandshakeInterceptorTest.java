package com.honjeong.chat.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.socket.WebSocketHandler;

/**
 * 핸드셰이크 인증. 여기를 통과하지 못하면 연결 자체가 성립하지 않아야 한다.
 */
class WsHandshakeInterceptorTest {

    private static ServerHttpRequest requestWith(String query) {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/ws");
        servletRequest.setQueryString(query);
        if (query != null && query.startsWith("ticket=")) {
            servletRequest.setParameter("ticket", query.substring("ticket=".length()));
        }
        return new ServletServerHttpRequest(servletRequest);
    }

    @Test
    @DisplayName("유효한 티켓이면 통과하고 userId를 세션에 남긴다")
    void acceptsValidTicket() throws Exception {
        WsTicketService tickets = mock(WsTicketService.class);
        when(tickets.consume("GOOD")).thenReturn(42L);
        WsHandshakeInterceptor interceptor = new WsHandshakeInterceptor(tickets);
        Map<String, Object> attributes = new HashMap<>();

        boolean allowed = interceptor.beforeHandshake(requestWith("ticket=GOOD"),
                mock(ServerHttpResponse.class), mock(WebSocketHandler.class), attributes);

        assertThat(allowed).isTrue();
        assertThat(attributes).containsEntry(WsHandshakeInterceptor.USER_ID_KEY, 42L);
    }

    @Test
    @DisplayName("★유효하지 않은 티켓이면 연결을 거부한다")
    void rejectsInvalidTicket() throws Exception {
        WsTicketService tickets = mock(WsTicketService.class);
        when(tickets.consume(anyString())).thenReturn(null);
        WsHandshakeInterceptor interceptor = new WsHandshakeInterceptor(tickets);
        Map<String, Object> attributes = new HashMap<>();

        boolean allowed = interceptor.beforeHandshake(requestWith("ticket=BAD"),
                mock(ServerHttpResponse.class), mock(WebSocketHandler.class), attributes);

        assertThat(allowed).isFalse();
        assertThat(attributes).isEmpty();
    }

    @Test
    @DisplayName("★티켓이 아예 없으면 거부한다")
    void rejectsMissingTicket() throws Exception {
        WsTicketService tickets = mock(WsTicketService.class);
        when(tickets.consume(null)).thenReturn(null);
        WsHandshakeInterceptor interceptor = new WsHandshakeInterceptor(tickets);
        Map<String, Object> attributes = new HashMap<>();

        boolean allowed = interceptor.beforeHandshake(requestWith(null),
                mock(ServerHttpResponse.class), mock(WebSocketHandler.class), attributes);

        assertThat(allowed).isFalse();
    }
}
