package com.honjeong.chat.dto;

/**
 * WebSocket 연결용 티켓.
 *
 * @param ticket           핸드셰이크 쿼리에 실을 불투명 문자열
 * @param expiresInSeconds 남은 수명(초) — 앱이 재발급 시점을 판단할 수 있게 알려준다
 */
public record WsTicketResponse(String ticket, int expiresInSeconds) {
}
