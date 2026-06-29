package com.honjeong.favorite.dto;

import java.util.List;

public record FavoriteGroupDetailResponse(
        Long groupId, String name, String note, String color, boolean isDefault,
        List<FavoritePlaceResponse> places) {
}
