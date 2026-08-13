package com.honjeong.chat.ws;

import java.time.Instant;

/**
 * 발급된 티켓 1건.
 *
 * @param userId   이 티켓으로 연결할 사용자
 * @param expiresAt 만료 시각(이 시각을 지나면 무효)
 */
record WsTicket(Long userId, Instant expiresAt) {

    boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }
}
