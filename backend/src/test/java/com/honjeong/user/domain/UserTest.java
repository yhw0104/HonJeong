package com.honjeong.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link User#updateProfile} 부분수정 변경자의 단위 테스트. 엔티티 자체의 규칙만 검증하므로 Spring·DB가 필요 없다.
 *
 * <p>핵심 규칙 3가지를 각각 확인한다: ① null 인자는 무시하고 기존 값을 보존, ② 빈 문자열("")은 해당 필드를 비움,
 * ③ {@code Boolean} 토글 반영. 모든 테스트는 프로필이 채워진 ACTIVE 회원에서 출발한다.
 */
class UserTest {

    /** 프로필이 모두 채워진 ACTIVE 회원을 만든다(부분수정 전 기준 상태). */
    private User activeUser() {
        User user = User.pending("01012345678", null);
        user.completeProfile("기존닉", Gender.MALE, "20s", "기존소개", "서울", 37.5, 127.0, DiningStyle.QUIET, "img.png");
        return user;
    }

    /**
     * given: 프로필이 채워진 회원.
     * when: 닉네임·식사성향만 값으로, 나머지는 null로 부분수정.
     * then: 전달한 필드만 바뀌고, null로 보낸 필드(소개·지역·수신토글)는 기존 값이 그대로 보존된다.
     */
    @Test
    @DisplayName("updateProfile: non-null 필드만 반영하고 null은 기존값을 보존한다")
    void updateProfile_appliesNonNullOnly() {
        User user = activeUser();

        user.updateProfile("새닉", null, null, null, null, null, DiningStyle.TALK, null);

        assertThat(user.getNickname()).isEqualTo("새닉");
        assertThat(user.getDiningStyle()).isEqualTo(DiningStyle.TALK);
        assertThat(user.getIntroduction()).isEqualTo("기존소개"); // null → 보존
        assertThat(user.getRegion()).isEqualTo("서울");          // null → 보존
        assertThat(user.isAllowMealRequest()).isTrue();          // null → 보존(기본 true)
    }

    /**
     * given: 자기소개가 채워진 회원.
     * when: introduction에 빈 문자열("")을 전달해 부분수정.
     * then: 빈 문자열은 null이 아니므로 그대로 반영되어 소개가 비워진다("비우기" 의미론).
     */
    @Test
    @DisplayName("updateProfile: 빈 문자열은 해당 필드를 비운다")
    void updateProfile_emptyStringClears() {
        User user = activeUser();

        user.updateProfile(null, null, "", null, null, null, null, null);

        assertThat(user.getIntroduction()).isEqualTo("");
    }

    /**
     * given: 같이먹기 수신이 기본 허용(true)인 회원.
     * when: allowMealRequest=false로 부분수정.
     * then: 엔티티 필드는 primitive지만 Boolean 인자로 토글이 정상 반영되어 false가 된다.
     */
    @Test
    @DisplayName("updateProfile: allowMealRequest=false 토글이 반영된다")
    void updateProfile_togglesAllowMealRequest() {
        User user = activeUser();

        user.updateProfile(null, null, null, null, null, null, null, Boolean.FALSE);

        assertThat(user.isAllowMealRequest()).isFalse();
    }
}
