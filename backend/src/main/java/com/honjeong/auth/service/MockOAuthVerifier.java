package com.honjeong.auth.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.honjeong.auth.domain.Provider;

/**
 * 1. 기능: 개발용 Mock OAuth 검증기 — 외부 호출 없이 idToken으로 결정론적 신원 생성(honjeong.oauth.mode=mock 또는 미지정 시 활성)
 * 2. 사용처: AuthService(oauthLogin) — OAuthVerifier 구현체로 주입
 *
 * <p>[기존 주석] 개발용 Mock 구현. 실제 카카오/애플 서버에 검증 요청을 보내지 않고, 받은 idToken을 그대로 이용해
 * <b>결정론적</b> 공급자 식별자를 만들어 낸다("mock-{공급자}-{idToken}"). 같은 idToken을 주면 항상 같은
 * 식별자가 나오므로, 같은 회원으로 매핑되어 반복 로그인 테스트가 손쉽다.
 *
 * <p>{@code @ConditionalOnProperty(..., matchIfMissing = true)}: 설정 {@code honjeong.oauth.mode}가
 * "mock"이거나 <b>아예 지정되지 않았을 때</b>(기본) 이 빈이 등록된다. 즉 개발 환경에서는 별도 설정 없이도
 * 이 Mock이 기본으로 쓰인다. 실 연동은 {@code honjeong.oauth.mode=real}로 두고 별도 구현으로 교체한다.
 */
@Component
@ConditionalOnProperty(name = "honjeong.oauth.mode", havingValue = "mock", matchIfMissing = true)
public class MockOAuthVerifier implements OAuthVerifier {

    /**
     * 기능: 실제 검증 없이 "mock-{공급자소문자}-{idToken}" 형식의 결정론적 신원 생성
     * Request: provider — 소셜 공급자, idToken — 공급자 발급 ID 토큰(식별자 합성에만 사용)
     * Response: OAuthIdentity — 합성 식별자, 이메일 null
     *
     * <p>[기존 주석] 실제 검증 없이 idToken으로 결정론적 신원을 만든다. 이메일은 제공하지 않으므로 null이다.
     *
     * @param provider 소셜 공급자
     * @param idToken  검증할 ID 토큰(실제 검증 없이 식별자 생성에 그대로 사용)
     * @return "mock-{공급자소문자}-{idToken}"을 식별자로, 이메일 null인 신원
     */
    @Override
    public OAuthIdentity verify(Provider provider, String idToken) {
        // 실제 토큰 검증 대신, 입력값만으로 항상 같은 식별자를 합성한다(테스트 재현성 확보).
        String providerUserId = "mock-" + provider.name().toLowerCase() + "-" + idToken;
        return new OAuthIdentity(provider, providerUserId, null);
    }
}
