package com.honjeong.favorite.dto;

public record FavoritePlaceResponse(
        Long placeId, String name, String category, String address, String roadAddress,
        double latitude, double longitude, boolean visited) {
}
