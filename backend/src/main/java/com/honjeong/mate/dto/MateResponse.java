package com.honjeong.mate.dto;

import java.time.LocalDateTime;

/**
 * 내 메이트 한 명의 정보를 나타내는 응답 DTO (GET /api/mates 목록 항목).
 */
public record MateResponse(
        Long mateUserId, // 메이트인 상대 사용자 ID
        String nickname, // 닉네임
        String profileImageUrl, // 프로필 이미지 URL(없으면 null)
        String diningStyle, // 식사 성향(TALK/QUIET/null)
        String region, // 활동 지역(없으면 null)
        boolean isOnline, // 현재 체크인 중(혼밥 중) 여부
        Long currentPlaceId, // 체크인 중인 장소 ID(오프라인이면 null)
        String currentPlaceName, // 체크인 중인 장소 이름(오프라인이면 null)
        LocalDateTime checkInStartedAt, // 현재 체크인 시작 시각(오프라인이면 null)
        long checkInCount, // 누적 체크인 횟수(취소 제외)
        long mealsTogether, // 나와 함께 먹은(같이먹기 수락) 횟수
        LocalDateTime matesSince) { // 메이트가 된 시각
}
