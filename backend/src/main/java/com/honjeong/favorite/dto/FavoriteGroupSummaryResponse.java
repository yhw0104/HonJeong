package com.honjeong.favorite.dto;

public record FavoriteGroupSummaryResponse(
        Long groupId, String name, String note, String color, boolean isDefault, long placeCount) {
}
