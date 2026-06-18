package com.honjeong.meal.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 같이먹기 신청 요청 본문. {@code POST /api/meal-requests}.
 *
 * @param toCheckInId 대상 혼밥러의 체크인 id(필수). place는 서버가 이 체크인에서 파생한다.
 * @param message     인사 한마디(선택, 최대 200자).
 */
public record MealRequestCreateRequest(
        @NotNull Long toCheckInId,
        @Size(max = 200) String message) {
}
