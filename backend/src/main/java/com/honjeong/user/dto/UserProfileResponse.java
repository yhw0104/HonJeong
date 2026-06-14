package com.honjeong.user.dto;

import com.honjeong.user.domain.DiningStyle;
import com.honjeong.user.domain.Gender;
import com.honjeong.user.domain.User;
import com.honjeong.user.domain.UserStatus;

/** 내 프로필 응답. {@code GET/PATCH /api/users/me}에서 반환. phone은 본인 프로필이므로 원문 그대로 노출(마스킹 없음). */
public record UserProfileResponse(
        Long id, String phone, String email, String nickname, String profileImageUrl,
        String introduction, String region, Double regionLat, Double regionLng,
        DiningStyle diningStyle, Gender gender, String ageGroup,
        boolean allowMealRequest, UserStatus status) {

    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(), user.getPhone(), user.getEmail(), user.getNickname(), user.getProfileImageUrl(),
                user.getIntroduction(), user.getRegion(), user.getRegionLat(), user.getRegionLng(),
                user.getDiningStyle(), user.getGender(), user.getAgeGroup(),
                user.isAllowMealRequest(), user.getStatus());
    }
}
