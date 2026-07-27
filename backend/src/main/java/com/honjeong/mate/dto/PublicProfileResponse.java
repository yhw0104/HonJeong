package com.honjeong.mate.dto;

import java.util.List;

/**
 * 다른 사용자의 공개 프로필 응답 DTO (GET /api/users/{id}/profile).
 *
 * @param userId           대상 사용자 ID
 * @param nickname         닉네임
 * @param profileImageUrl  프로필 이미지 URL(없으면 null)
 * @param introduction     자기소개(없으면 null)
 * @param region           활동 지역(없으면 null)
 * @param gender           성별(없으면 null)
 * @param ageGroup         연령대(없으면 null)
 * @param diningStyle      식사 성향(TALK/QUIET/null)
 * @param preferredFoods   선호 음식 목록
 * @param checkInCount     누적 체크인 횟수(취소 제외)
 * @param mealsTogether    조회자와 함께 먹은(같이먹기 수락) 횟수
 * @param badgeCount       뱃지 수(대상 사용자가 획득한 뱃지 개수)
 * @param isOnline         현재 체크인 중 여부
 * @param currentPlaceName 체크인 중인 장소 이름(오프라인이면 null)
 * @param currentPlaceId   체크인 중인 장소 ID(오프라인이면 null)
 * @param isMate           조회자와 메이트인지 여부
 * @param requestStatus    조회자와의 신청 관계(NONE/PENDING_SENT/PENDING_RECEIVED)
 */
public record PublicProfileResponse(
        Long userId, String nickname, String profileImageUrl, String introduction,
        String region, String gender, String ageGroup, String diningStyle,
        List<String> preferredFoods,
        long checkInCount, long mealsTogether, long badgeCount,
        boolean isOnline, String currentPlaceName, Long currentPlaceId,
        boolean isMate, String requestStatus) {
}
