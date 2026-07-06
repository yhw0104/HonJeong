package com.honjeong.auth.domain;

/**
 * 소셜 로그인 공급자. SocialAccount.provider에 @Enumerated(EnumType.STRING)으로 이름 그대로 저장되고,
 * (provider, providerUserId) 조합으로 회원을 식별한다.
 */
public enum Provider {
    /** 카카오 로그인 */
    KAKAO,
    /** 애플 로그인 */
    APPLE
}
