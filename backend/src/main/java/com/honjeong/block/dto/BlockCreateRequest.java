package com.honjeong.block.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 차단 생성 요청 데이터 (POST /api/blocks 요청 바디)
 *
 * <p>[기존 주석] 차단 생성 요청.
 *
 * @param targetUserId 차단할 유저 ID (필수)
 */
public record BlockCreateRequest(@NotNull Long targetUserId) {
}
