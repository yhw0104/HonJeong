package com.honjeong.mate.dto;

public record UserSearchResponse(
        Long userId, String nickname, String profileImageUrl, String region,
        boolean isMate, String requestStatus) {
}
