package com.honjeong.chat.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

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
import com.honjeong.user.domain.UserStatus;

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

    @Test
    @DisplayName("★정지된 회원은 401 ACCOUNT_INACTIVE — 티켓이 없으면 소켓에도 못 붙는다")
    void rejectsSuspended() throws Exception {
        // 이 테스트가 설계의 핵심 주장을 실제로 증명한다: WS 경로에 계정 상태 검사를 따로 구현하지
        // 않아도 되는 이유는, 유일한 입구인 티켓 발급이 일반 REST라 ActiveUserFilter를 그대로 타기
        // 때문이다. 그 주장이 네 군데 Javadoc에 적혀 있는데 지금까지 잠가 두는 테스트가 없었다.
        // (ActiveUserSliceSupport의 기본 ACTIVE 스텁을 이 id에 대해서만 덮어쓴다.)
        when(userRepository.findStatusById(1L)).thenReturn(Optional.of(UserStatus.SUSPENDED));

        mockMvc.perform(post("/api/ws-ticket").header("Authorization", userToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_INACTIVE"));

        // 발급 로직까지 가지 않고 필터에서 끊겼는지 확인한다 — 401을 내면서도 티켓을 만들어 뒀다면
        // 그 티켓으로 핸드셰이크가 통과해 버린다.
        verify(wsTicketService, never()).issue(any());
    }
}
