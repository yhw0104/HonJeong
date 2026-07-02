package com.honjeong.mate.dto;

import java.time.LocalDateTime;

public record MateResponse(
        Long mateUserId,
        String nickname,
        String profileImageUrl,
        String diningStyle,
        String region,
        boolean isOnline,
        Long currentPlaceId,
        String currentPlaceName,
        LocalDateTime checkInStartedAt,
        long checkInCount,
        long mealsTogether,
        LocalDateTime matesSince) {
}
