package com.honjeong.chat.dto;

import java.time.LocalDateTime;

/**
 * 대화 목록 1행. partnerProfileImageUrl은 없을 수 있음(null).
 * partnerLastReadAt = 상대가 마지막으로 읽은 시각(내 메시지의 '읽음' 표시용, 아직 안 읽었으면 null).
 * createdAt = 대화방이 열린(매칭 성사) 시각 — 메시지가 아직 없어 lastMessageAt이 null일 때 목록에 대신 표시한다.
 */
public record ConversationSummaryResponse(
        Long conversationId, String status,
        Long partnerUserId, String partnerNickname, String partnerProfileImageUrl,
        String placeName, String lastMessagePreview, LocalDateTime lastMessageAt, long unreadCount,
        LocalDateTime partnerLastReadAt, LocalDateTime createdAt) {
}
