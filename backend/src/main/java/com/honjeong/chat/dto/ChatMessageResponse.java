package com.honjeong.chat.dto;

import java.time.LocalDateTime;

import com.honjeong.chat.domain.ChatMessage;

/** 대화 메시지 1건 응답. type은 ChatMessage.MessageType의 name()("TEXT"/"IMAGE"). */
public record ChatMessageResponse(
        Long id, Long senderUserId, String type, String text, String imageUrl, LocalDateTime createdAt) {
    public static ChatMessageResponse from(ChatMessage m) {
        return new ChatMessageResponse(m.getId(), m.getSenderUserId(), m.getType().name(),
                m.getText(), m.getImageUrl(), m.getCreatedAt());
    }
}
