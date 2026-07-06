package com.honjeong.favorite.dto;

/**
 * 즐겨찾기 그룹 요약(목록 카드용) 응답 — 그룹 목록 조회·생성·수정 API가 공통으로 반환.
 *
 * @param groupId    그룹 ID
 * @param name       그룹 이름
 * @param note       그룹 메모 (없으면 null)
 * @param color      그룹 색상 HEX
 * @param isDefault  기본 그룹 여부
 * @param placeCount 그룹에 담긴 장소 수
 */
public record FavoriteGroupSummaryResponse(
        Long groupId, String name, String note, String color, boolean isDefault, long placeCount) {
}
