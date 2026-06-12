package com.honjeong.auth.service;

import com.honjeong.auth.domain.Provider;

/**
 * 공급자 토큰 검증 결과로 얻는 소셜 신원을 담은 불변 값 객체(record). {@link OAuthVerifier#verify}가 만들어
 * 돌려주며, {@link AuthService#oauthLogin}이 이 값으로 회원을 찾거나 새로 만든다.
 *
 * @param provider       소셜 공급자(KAKAO·APPLE 등)
 * @param providerUserId 공급자가 부여한 사용자 고유 식별자(JWT의 sub). (공급자, 이 값) 조합으로 소셜 계정을
 *                       유일하게 식별한다.
 * @param email          이메일(공급자가 제공할 때만 — 선택값, 없으면 null)
 */
public record OAuthIdentity(Provider provider, String providerUserId, String email) {
}
