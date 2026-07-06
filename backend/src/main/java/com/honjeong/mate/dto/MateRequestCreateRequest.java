package com.honjeong.mate.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 메이트 신청 생성 요청 바디 (POST /api/mate-requests).
 *
 * @param toUserId 메이트 신청을 받을 상대 사용자 ID (필수)
 */
public record MateRequestCreateRequest(@NotNull Long toUserId) {
}
