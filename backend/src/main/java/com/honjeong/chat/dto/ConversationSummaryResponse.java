package com.honjeong.chat.dto;

import java.time.LocalDateTime;

/** 대화 목록 1행. partnerProfileImageUrl은 없을 수 있음(null). */
public record ConversationSummaryResponse(
        Long conversationId, String status,
        Long partnerUserId, String partnerNickname, String partnerProfileImageUrl,
        String placeName, String lastMessagePreview, LocalDateTime lastMessageAt, long unreadCount) {
}
