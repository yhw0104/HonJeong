package com.honjeong.mate.dto;

import java.util.List;

public record PublicProfileResponse(
        Long userId, String nickname, String profileImageUrl, String introduction,
        String region, String gender, String ageGroup, String diningStyle,
        List<String> preferredFoods,
        long checkInCount, long mealsTogether, long badgeCount,
        boolean isOnline, String currentPlaceName, Long currentPlaceId,
        boolean isMate, String requestStatus) {
}
