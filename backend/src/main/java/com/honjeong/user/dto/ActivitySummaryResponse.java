package com.honjeong.user.dto;

/**
 * 프로필 카드 활동요약 응답. 카운트 숫자만 담는 확장형 DTO —
 * 지표 추가 시 필드 한 줄 추가로 끝난다(필드 추가는 하위호환).
 *
 * @param checkInCount 사용자의 전체 체크인 수(혼밥 횟수)
 * @param favoriteCount 사용자가 담은 고유 식당 수(즐겨찾기)
 * @param mateCount 메이트 수 — 메이트 도메인 도입 전까지 항상 0
 */
public record ActivitySummaryResponse(long checkInCount, long favoriteCount, long mateCount) {
}
