package com.honjeong.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** {@link User#updateProfile} 부분수정 규칙 단위 테스트(null=무시, 빈문자열 비우기, 토글). */
class UserTest {

    private User activeUser() {
        User user = User.pending("01012345678", null);
        user.completeProfile("기존닉", Gender.MALE, "20s", "기존소개", "서울", 37.5, 127.0, DiningStyle.QUIET, "img.png");
        return user;
    }

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

    @Test
    @DisplayName("updateProfile: 빈 문자열은 해당 필드를 비운다")
    void updateProfile_emptyStringClears() {
        User user = activeUser();

        user.updateProfile(null, null, "", null, null, null, null, null);

        assertThat(user.getIntroduction()).isEqualTo("");
    }

    @Test
    @DisplayName("updateProfile: allowMealRequest=false 토글이 반영된다")
    void updateProfile_togglesAllowMealRequest() {
        User user = activeUser();

        user.updateProfile(null, null, null, null, null, null, null, Boolean.FALSE);

        assertThat(user.isAllowMealRequest()).isFalse();
    }
}
