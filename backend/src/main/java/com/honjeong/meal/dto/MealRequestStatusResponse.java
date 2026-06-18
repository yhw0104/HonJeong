package com.honjeong.meal.dto;

import java.time.LocalDateTime;

import com.honjeong.meal.domain.MealRequest;

/**
 * 같이먹기 신청 수락/거절 응답(200).
 *
 * @param mealRequestId 신청 id
 * @param status        전이된 상태(ACCEPTED|DECLINED)
 * @param respondedAt   응답 시각
 */
public record MealRequestStatusResponse(Long mealRequestId, String status, LocalDateTime respondedAt) {

    public static MealRequestStatusResponse from(MealRequest mr) {
        return new MealRequestStatusResponse(mr.getId(), mr.getStatus().name(), mr.getRespondedAt());
    }
}
