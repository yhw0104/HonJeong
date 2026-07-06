package com.honjeong.favorite.dto;

import java.util.List;

/**
 * 즐겨찾기 그룹 상세(그룹 정보 + 담긴 장소 목록) 응답 (GET /api/favorite-groups/{groupId}).
 *
 * @param groupId   그룹 ID
 * @param name      그룹 이름
 * @param note      그룹 메모 (없으면 null)
 * @param color     그룹 색상 HEX
 * @param isDefault 기본 그룹 여부 (기본 그룹은 삭제 불가)
 * @param places    그룹에 담긴 장소 목록 (각 장소의 방문 여부 포함, 최신 담김 순)
 */
public record FavoriteGroupDetailResponse(
        Long groupId, String name, String note, String color, boolean isDefault,
        List<FavoritePlaceResponse> places) {
}
