package com.honjeong.mate.dto;

/**
 * 사용자 검색 결과 한 건. {@code GET /api/users/search}의 목록 항목이다.
 *
 * @param userId          대상 사용자 id
 * @param nickname        닉네임
 * @param profileImageUrl 프로필 이미지 URL(없으면 null)
 * @param region          활동 지역 표시명(없으면 null) — 표시는 중단됐지만 하위호환으로 유지
 * @param diningStyle     식사 성향({@code TALK}/{@code QUIET}/null) — 검색 카드 표시용
 * @param isMate          나와 이미 메이트인지
 * @param requestStatus   나와의 신청 관계({@code NONE}/{@code PENDING_SENT}/{@code PENDING_RECEIVED})
 */
public record UserSearchResponse(
        Long userId, String nickname, String profileImageUrl, String region, String diningStyle,
        boolean isMate, String requestStatus) {
}
