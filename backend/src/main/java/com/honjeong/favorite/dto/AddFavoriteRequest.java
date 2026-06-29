package com.honjeong.favorite.dto;

import jakarta.validation.constraints.NotNull;

public record AddFavoriteRequest(@NotNull Long placeId) {
}
