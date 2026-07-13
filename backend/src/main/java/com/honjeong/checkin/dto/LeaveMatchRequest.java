package com.honjeong.checkin.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 같이먹기 매칭 깨기 요청. {@code to}는 전이할 내 상태 문자열(ACTIVE=혼밥 계속 / SEEKING=다시 모집 / CANCELLED=취소).
 * enum 파싱·허용값 검증은 서비스에서 한다(잘못되면 400 INVALID_INPUT).
 *
 * @param to 전이할 내 상태(ACTIVE/SEEKING/CANCELLED)
 */
public record LeaveMatchRequest(@NotBlank String to) {
}
