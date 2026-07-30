package com.honjeong.meal.dto;

import java.time.LocalDateTime;

import com.honjeong.global.common.DisplayNames;
import com.honjeong.meal.domain.MealRequest;

/**
 * 같이먹기 신청 목록 항목(GET). 받은 목록은 fromUser(신청자), 보낸 목록은 toUser(상대)를 화면이 골라 표시한다.
 * 상대 닉네임은 해당 식당 혼밥러 목록에 이미 공개되므로 노출에 추가 프라이버시 누출이 없다.
 *
 * @param mealRequestId 신청 id
 * @param fromUser      신청자(userId + 닉네임)
 * @param toUser        대상 수신자(userId + 닉네임)
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

    /** 신청자 요약(userId + 닉네임 + 프로필 사진). 사진이 없으면 null이고 앱이 앱 아이콘으로 대체한다. */
    public record FromUser(Long userId, String nickname, String profileImageUrl) {
    }

    /** 수신자 요약(userId + 닉네임 + 프로필 사진). 사진이 없으면 null이고 앱이 앱 아이콘으로 대체한다. */
    public record ToUser(Long userId, String nickname, String profileImageUrl) {
    }

    public static MealRequestListItemResponse from(MealRequest mr) {
        return new MealRequestListItemResponse(
                mr.getId(),
                // 탈퇴자는 닉네임이 null이라 '알 수 없음'으로 표시한다(DisplayNames). 사진은 탈퇴 시 파일까지
                // 지워지고 URL도 null이 되므로(AccountWithdrawalService) 그대로 내려보내면 앱이 앱 아이콘을 쓴다.
                new FromUser(mr.getFromUser().getId(),
                        DisplayNames.nicknameOrUnknown(mr.getFromUser().getNickname()),
                        mr.getFromUser().getProfileImageUrl()),
                new ToUser(mr.getToCheckIn().getUser().getId(),
                        DisplayNames.nicknameOrUnknown(mr.getToCheckIn().getUser().getNickname()),
                        mr.getToCheckIn().getUser().getProfileImageUrl()),
                mr.getPlace().getId(),
                mr.getPlace().getName(),
                mr.getMessage(),
                mr.getStatus().name(),
                mr.getCreatedAt());
    }
}
