package com.honjeong.meal.dto;

import java.time.LocalDateTime;

import com.honjeong.meal.domain.MealRequest;

/**
 * 같이먹기 신청 목록 항목(GET). 프라이버시상 신청자는 닉네임만 노출한다.
 *
 * @param mealRequestId 신청 id
 * @param fromUser      신청자(닉네임만)
 * @param placeId       신청 발생 장소 id
 * @param message       인사 한마디(nullable)
 * @param status        상태 문자열
 * @param createdAt     신청 시각
 */
public record MealRequestListItemResponse(
        Long mealRequestId,
        FromUser fromUser,
        Long placeId,
        String message,
        String status,
        LocalDateTime createdAt) {

    /** 신청자 요약(닉네임만). */
    public record FromUser(String nickname) {
    }

    public static MealRequestListItemResponse from(MealRequest mr) {
        return new MealRequestListItemResponse(
                mr.getId(),
                new FromUser(mr.getFromUser().getNickname()),
                mr.getPlace().getId(),
                mr.getMessage(),
                mr.getStatus().name(),
                mr.getCreatedAt());
    }
}
