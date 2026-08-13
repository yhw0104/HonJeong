package com.honjeong.chat.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.honjeong.chat.ws.WsTicketService;
import com.honjeong.global.config.SecurityConfig;
import com.honjeong.global.config.WebConfig;
import com.honjeong.global.security.JwtProvider;
import com.honjeong.support.ActiveUserSliceSupport;

/**
 * 티켓 발급 엔드포인트.
 *
 * <p>이 엔드포인트가 일반 REST인 것이 설계의 핵심이다 — SecurityFilterChain과 ActiveUserFilter를
 * 그대로 타므로, 미인증·탈퇴·정지 사용자 차단이 WebSocket 경로에 자동으로 적용된다.
 */
@WebMvcTest(controllers = WsTicketController.class)
@Import({SecurityConfig.class, WebConfig.class})
class WsTicketControllerTest extends ActiveUserSliceSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private WsTicketService wsTicketService;

    private String userToken() {
        return "Bearer " + jwtProvider.createAccessToken(1L);
    }

    @Test
    @DisplayName("로그인 사용자는 티켓을 받는다")
    void issuesTicket() throws Exception {
        when(wsTicketService.issue(eq(1L))).thenReturn("TICKET-ABC");
        when(wsTicketService.ttlSeconds()).thenReturn(99);

        mockMvc.perform(post("/api/ws-ticket").header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticket").value("TICKET-ABC"))
                .andExpect(jsonPath("$.data.expiresInSeconds").value(99));
    }

    @Test
    @DisplayName("★미인증이면 401 — 티켓을 아무나 받을 수 없다")
    void rejectsAnonymous() throws Exception {
        mockMvc.perform(post("/api/ws-ticket"))
                .andExpect(status().isUnauthorized());
    }
}
