package com.honjeong.user.dto;

/**
 * 프로필 카드 활동요약 응답. 카운트 숫자만 담는 확장형 DTO —
 * 지표 추가 시 필드 한 줄 추가로 끝난다(필드 추가는 하위호환).
 *
 * @param checkInCount 혼밥(혼자 먹은) 횟수 — 매칭 안 되고 혼자 먹은 완료 체크인 수(matchedAt IS NULL)
 * @param reviewCount 사용자가 작성한 인증 리뷰(체크인 기반 혼밥일기) 수 — 혼밥 기록 화면 기준과 일치
 * @param favoriteCount 사용자가 담은 고유 식당 수(즐겨찾기)
 * @param mateCount 메이트 수 — 메이트 도메인 도입 전까지 항상 0
 * @param togetherCount 같이먹음(매칭돼 같이 먹은) 횟수 — matchedAt IS NOT NULL
 */
public record ActivitySummaryResponse(
        long checkInCount, long reviewCount, long favoriteCount, long mateCount, long togetherCount) {
}
