package com.honjeong.meal.dto;

import com.honjeong.meal.domain.MealRequest;

/**
 * 같이먹기 신청 생성 응답(POST 201).
 *
 * @param mealRequestId 신청 id
 * @param toCheckInId   대상 체크인 id
 * @param message       인사 한마디(nullable)
 * @param status        상태 문자열(PENDING)
 */
public record MealRequestResponse(Long mealRequestId, Long toCheckInId, String message, String status) {

    public static MealRequestResponse from(MealRequest mr) {
        return new MealRequestResponse(mr.getId(), mr.getToCheckIn().getId(), mr.getMessage(), mr.getStatus().name());
    }
}
