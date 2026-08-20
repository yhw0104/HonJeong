package com.honjeong.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SocialAccountTest {

    @Test
    @DisplayName("새 소셜 계정의 애플 refresh token은 비어 있다")
    void 처음에는_비어있다() {
        SocialAccount account = SocialAccount.of(1L, Provider.APPLE, "apple-sub", null);

        assertThat(account.getAppleRefreshToken()).isNull();
    }

    @Test
    @DisplayName("애플 refresh token을 붙일 수 있다")
    void 토큰을_붙인다() {
        SocialAccount account = SocialAccount.of(1L, Provider.APPLE, "apple-sub", null);

        account.attachAppleRefreshToken("r-token");

        assertThat(account.getAppleRefreshToken()).isEqualTo("r-token");
    }
}
