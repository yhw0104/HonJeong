package com.honjeong.user.dto;

import com.honjeong.user.domain.DiningStyle;
import com.honjeong.user.domain.Gender;
import com.honjeong.user.domain.User;
import com.honjeong.user.domain.UserStatus;

/**
 * 내 프로필 응답 본문. {@code GET /api/users/me}와 {@code PATCH /api/users/me}에서 반환한다.
 *
 * <p>본인 프로필 화면에 필요한 전 필드를 노출한다. {@code phone}은 본인 프로필이므로 마스킹하지 않고 원문 그대로
 * 내려준다. {@code diningStyle}/{@code gender}/{@code status}는 enum 이름 문자열로 직렬화된다.
 *
 * @param id               회원 식별자(PK)
 * @param phone            휴대폰 번호(원문, 마스킹 없음)
 * @param email            이메일(소셜 로그인 시 제공될 수 있음, 없으면 null)
 * @param nickname         닉네임
 * @param profileImageUrl  프로필 이미지 URL
 * @param introduction     한 줄 소개
 * @param region           활동 지역 표시명
 * @param regionLat        활동 지역 위도
 * @param regionLng        활동 지역 경도
 * @param diningStyle      식사 성향({@link DiningStyle})
 * @param gender           성별({@link Gender})
 * @param ageGroup         연령대 문자열
 * @param allowMealRequest 같이먹기 신청 수신 허용 여부
 * @param status           회원 상태({@link UserStatus})
 */
public record UserProfileResponse(
        Long id, String phone, String email, String nickname, String profileImageUrl,
        String introduction, String region, Double regionLat, Double regionLng,
        DiningStyle diningStyle, Gender gender, String ageGroup,
        boolean allowMealRequest, UserStatus status) {

    /**
     * {@link User} 엔티티를 프로필 응답 DTO로 변환한다. 엔티티의 게터 값을 1:1로 옮겨 담을 뿐 가공은 없다.
     *
     * @param user 변환할 회원 엔티티
     * @return 엔티티의 필드 값을 담은 새 {@link UserProfileResponse}
     */
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(), user.getPhone(), user.getEmail(), user.getNickname(), user.getProfileImageUrl(),
                user.getIntroduction(), user.getRegion(), user.getRegionLat(), user.getRegionLng(),
                user.getDiningStyle(), user.getGender(), user.getAgeGroup(),
                user.isAllowMealRequest(), user.getStatus());
    }
}
