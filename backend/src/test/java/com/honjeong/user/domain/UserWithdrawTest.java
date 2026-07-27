package com.honjeong.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserWithdrawTest {

    @Test
    @DisplayName("탈퇴하면 개인정보 필드가 전부 비워지고 상태가 WITHDRAWN이 된다")
    void withdrawClearsPersonalData() {
        User u = User.pending("01012345678", "a@b.com");
        u.completeProfile("혼밥러", Gender.MALE, LocalDate.of(1995, 3, 2), "안녕하세요",
                "서울 강남구", 37.5, 127.0, DiningStyle.QUIET, "http://localhost:8080/files/a.jpg");

        u.withdraw();

        assertThat(u.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
        assertThat(u.getPhone()).isNull();
        assertThat(u.getEmail()).isNull();
        assertThat(u.getNickname()).isNull();
        assertThat(u.getProfileImageUrl()).isNull();
        assertThat(u.getGender()).isNull();
        assertThat(u.getBirthDate()).isNull();
        assertThat(u.getIntroduction()).isNull();
        assertThat(u.getRegion()).isNull();
        assertThat(u.getRegionLat()).isNull();
        assertThat(u.getRegionLng()).isNull();
        assertThat(u.getDiningStyle()).isNull();
        assertThat(u.isAllowMealRequest()).isFalse();
    }
}
