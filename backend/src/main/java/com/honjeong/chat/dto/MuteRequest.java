package com.honjeong.chat.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 대화 음소거 토글 본문.
 *
 * @param muted true면 이 대화의 푸시를 받지 않는다
 */
public record MuteRequest(@NotNull Boolean muted) {
}
