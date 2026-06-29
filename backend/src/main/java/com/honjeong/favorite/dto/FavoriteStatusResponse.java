package com.honjeong.favorite.dto;

import java.util.List;

public record FavoriteStatusResponse(boolean saved, List<FavoriteStatusGroup> groups) {
}
