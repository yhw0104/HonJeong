package com.honjeong.chat.dto;

import com.honjeong.chat.domain.MessageType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** TEXT면 text 필수(≤1000)·imageUrl null, IMAGE면 imageUrl 필수·text null. 상호배타는 서비스에서 검증. */
public record SendMessageRequest(
        @NotNull MessageType type,
        @Size(max = 1000) String text,
        @Size(max = 500) String imageUrl) {
}
