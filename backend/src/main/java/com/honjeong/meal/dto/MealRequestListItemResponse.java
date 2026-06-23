package com.honjeong.meal.dto;

import java.time.LocalDateTime;

import com.honjeong.meal.domain.MealRequest;

/**
 * 같이먹기 신청 목록 항목(GET). 받은 목록은 fromUser(신청자), 보낸 목록은 toUser(상대)를 화면이 골라 표시한다.
 * 상대 닉네임은 해당 식당 혼밥러 목록에 이미 공개되므로 노출에 추가 프라이버시 누출이 없다.
 *
 * @param mealRequestId 신청 id
 * @param fromUser      신청자(닉네임만)
 * @param toUser        대상 수신자(닉네임만)
 * @param placeId       신청 발생 장소 id
 * @param placeName     장소 이름
 * @param message       인사 한마디(nullable)
 * @param status        상태 문자열
 * @param createdAt     신청 시각
 */
public record MealRequestListItemResponse(
        Long mealRequestId,
        FromUser fromUser,
        ToUser toUser,
        Long placeId,
        String placeName,
        String message,
        String status,
        LocalDateTime createdAt) {

    /** 신청자 요약(닉네임만). */
    public record FromUser(String nickname) {
    }

    /** 수신자 요약(닉네임만). */
    public record ToUser(String nickname) {
    }

    public static MealRequestListItemResponse from(MealRequest mr) {
        return new MealRequestListItemResponse(
                mr.getId(),
                new FromUser(mr.getFromUser().getNickname()),
                new ToUser(mr.getToCheckIn().getUser().getNickname()),
                mr.getPlace().getId(),
                mr.getPlace().getName(),
                mr.getMessage(),
                mr.getStatus().name(),
                mr.getCreatedAt());
    }
}
