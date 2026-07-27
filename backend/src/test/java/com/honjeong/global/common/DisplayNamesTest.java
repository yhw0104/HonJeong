package com.honjeong.global.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DisplayNamesTest {

    @Test
    @DisplayName("닉네임이 있으면 그대로 돌려준다")
    void keepsExistingNickname() {
        assertThat(DisplayNames.nicknameOrUnknown("혼밥러")).isEqualTo("혼밥러");
    }

    @Test
    @DisplayName("탈퇴로 닉네임이 없으면 '알 수 없음'")
    void nullBecomesUnknown() {
        assertThat(DisplayNames.nicknameOrUnknown(null)).isEqualTo("알 수 없음");
    }

    @Test
    @DisplayName("빈 문자열·공백도 '알 수 없음'으로 본다")
    void blankBecomesUnknown() {
        assertThat(DisplayNames.nicknameOrUnknown("")).isEqualTo("알 수 없음");
        assertThat(DisplayNames.nicknameOrUnknown("   ")).isEqualTo("알 수 없음");
    }
}
