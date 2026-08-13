package com.honjeong.chat.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.honjeong.chat.dto.WsTicketResponse;
import com.honjeong.chat.ws.WsTicketService;
import com.honjeong.global.common.ApiResponse;
import com.honjeong.global.security.CurrentUserId;

/**
 * WebSocket 연결 전 단계 — 1회용 티켓을 발급한다.
 *
 * <p>★ 이 엔드포인트가 <b>일반 REST</b>인 것이 설계의 핵심이다. SecurityFilterChain과
 * {@code ActiveUserFilter}를 그대로 타므로, 미인증·탈퇴·정지 사용자 차단이 WebSocket 경로에
 * 자동으로 적용된다. 핸드셰이크에서 직접 인증했다면 그 판정을 한 벌 더 구현해야 했고,
 * 두 벌이 된 규칙은 반드시 갈린다.
 */
@RestController
@RequestMapping("/api/ws-ticket")
public class WsTicketController {

    private final WsTicketService wsTicketService;

    public WsTicketController(WsTicketService wsTicketService) {
        this.wsTicketService = wsTicketService;
    }

    /**
     * 티켓을 발급한다.
     *
     * <p>남은 수명은 상수로 따로 들고 있지 않고 {@code wsTicketService.ttlSeconds()}에서 그대로
     * 가져온다 — 값을 두 곳에 적으면 서비스 쪽 TTL이 바뀌었을 때 응답만 옛 값을 계속 돌려줄 수
     * 있고, 그러면 클라이언트가 이미 죽은 티켓을 붙들고 있거나 아직 살아 있는 티켓을 일찍
     * 버리게 된다. 단일 진실 공급원을 서비스로 두면 이 어긋남 자체가 불가능해진다.
     *
     * @param userId 로그인 사용자 id
     * @return 티켓과 남은 수명
     */
    @PostMapping
    public ApiResponse<WsTicketResponse> issue(@CurrentUserId Long userId) {
        return ApiResponse.success(new WsTicketResponse(wsTicketService.issue(userId), wsTicketService.ttlSeconds()));
    }
}
