package com.honjeong.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.honjeong.auth.domain.Provider;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;

/** 공급자에 따라 올바른 검증기로 넘기는지만 본다. 각 검증기의 검증 규칙은 각자의 테스트가 담당한다. */
class DelegatingOAuthVerifierTest {

    private KakaoOAuthVerifier kakao;
    private AppleOAuthVerifier apple;
    private DelegatingOAuthVerifier delegating;

    @BeforeEach
    void setUp() {
        kakao = mock(KakaoOAuthVerifier.class);
        apple = mock(AppleOAuthVerifier.class);
        delegating = new DelegatingOAuthVerifier(kakao, apple);
    }

    @Test
    @DisplayName("KAKAO 요청은 카카오 검증기로 간다 — 애플 검증기는 호출되지 않는다")
    void 카카오는_카카오검증기로() {
        OAuthIdentity expected = new OAuthIdentity(Provider.KAKAO, "kakao-sub", null);
        when(kakao.verify(eq(Provider.KAKAO), eq("token"))).thenReturn(expected);

        assertThat(delegating.verify(Provider.KAKAO, "token")).isEqualTo(expected);
        verify(apple, never()).verify(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("APPLE 요청은 애플 검증기로 간다 — 카카오 검증기는 호출되지 않는다")
    void 애플은_애플검증기로() {
        OAuthIdentity expected = new OAuthIdentity(Provider.APPLE, "apple-sub", null);
        when(apple.verify(eq(Provider.APPLE), eq("token"))).thenReturn(expected);

        assertThat(delegating.verify(Provider.APPLE, "token")).isEqualTo(expected);
        verify(kakao, never()).verify(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("provider가 null이면 거부한다 — 조용히 아무 검증기로 보내지 않는다")
    void null_공급자는_거부한다() {
        assertThatThrownBy(() -> delegating.verify(null, "token"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }
}
