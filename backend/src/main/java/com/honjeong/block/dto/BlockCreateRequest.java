package com.honjeong.block.dto;

import jakarta.validation.constraints.NotNull;

/** 차단 생성 요청. */
public record BlockCreateRequest(@NotNull Long targetUserId) {
}
