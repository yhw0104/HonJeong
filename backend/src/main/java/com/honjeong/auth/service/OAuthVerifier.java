package com.honjeong.auth.service;

import com.honjeong.auth.domain.Provider;

/**
 * 소셜 로그인 검증의 책임을 정의하는 인터페이스. 공급자(카카오·애플 등)가 발급한 idToken을 검증해,
 * 그 토큰이 가리키는 공급자 측 신원({@link OAuthIdentity})을 돌려준다. 공급자 토큰 자체는 저장하지 않고
 * 검증에만 쓴다.
 *
 * <p>사용처: AuthService(oauthLogin). 구현체는 개발용 {@link MockOAuthVerifier}와 운영용
 * {@link KakaoOAuthVerifier}가 있고, {@code honjeong.oauth.mode}로 갈린다.
 *
 * <p>실제 검증 로직(공급자별 공개키로 서명 확인 등)은 환경에 따라 구현이 갈린다 — 개발용
 * {@link MockOAuthVerifier}는 외부 호출 없이 결정론적 식별자를 만들고, 실 운영용 구현은 공급자 API와
 * 통신한다. 이렇게 인터페이스로 추상화해 {@link AuthService}는 구현 교체에 영향받지 않는다.
 */
public interface OAuthVerifier {

    /**
     * 공급자 토큰을 검증해 신원을 추출한다.
     *
     * @param provider 소셜 공급자(KAKAO·APPLE 등)
     * @param idToken  공급자가 발급한 ID 토큰
     * @return 검증된 공급자 측 신원(공급자 고유 식별자·이메일 등)
     */
    OAuthIdentity verify(Provider provider, String idToken);
}
