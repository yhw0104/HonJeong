package com.honjeong.favorite.dto;

import java.util.List;

/**
 * 특정 장소에 대한 사용자의 즐겨찾기 상태 응답 (GET /api/places/{placeId}/favorite-status).
 *
 * @param saved  즐겨찾기 여부 (하나 이상의 그룹에 담겨 있으면 true)
 * @param groups 사용자의 전체 그룹 목록과 각 그룹의 담김 여부
 */
public record FavoriteStatusResponse(boolean saved, List<FavoriteStatusGroup> groups) {
}
