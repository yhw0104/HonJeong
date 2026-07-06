package com.honjeong.favorite.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 그룹에 장소를 담을 때 사용하는 요청 바디 (POST /api/favorite-groups/{groupId}/places).
 *
 * @param placeId 즐겨찾기에 추가할 장소 ID (필수)
 */
public record AddFavoriteRequest(@NotNull Long placeId) {
}
