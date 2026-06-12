package com.honjeong.auth.service;

/**
 * 인증 진입(휴대폰 인증·OAuth)의 결과를 나타내는 불변 값 객체(record). 진입 결과는 둘 중 하나이며,
 * {@code onboarding} 플래그로 어느 쪽인지 구분한다.
 *
 * <ul>
 *   <li>온보딩이 필요한 경우(신규·미완 회원): {@code onboarding=true}, {@code onboardingToken}만 채워지고
 *       {@code tokens}는 null이다.</li>
 *   <li>바로 로그인되는 경우(기존 ACTIVE 회원): {@code onboarding=false}, {@code tokens}만 채워지고
 *       {@code onboardingToken}은 null이다.</li>
 * </ul>
 * 두 팩토리 메서드로만 생성해 위 불변식(둘 중 하나만 채워짐)을 강제한다.
 *
 * @param onboarding      온보딩이 필요한지 여부(true면 온보딩, false면 즉시 로그인)
 * @param onboardingToken 온보딩 단계에서 쓰는 임시 토큰(온보딩일 때만 값이 있음)
 * @param tokens          정식 토큰 쌍(즉시 로그인일 때만 값이 있음)
 */
public record AuthResult(boolean onboarding, String onboardingToken, TokenPair tokens) {

    /**
     * 온보딩이 필요한 결과를 만든다. 신규 회원이거나 아직 프로필을 끝내지 않은 회원에게 사용한다.
     *
     * @param onboardingToken 온보딩 단계 진행에 쓸 임시 토큰
     * @return onboarding=true, tokens=null인 결과
     */
    public static AuthResult onboarding(String onboardingToken) {
        return new AuthResult(true, onboardingToken, null);
    }

    /**
     * 즉시 로그인 결과를 만든다. 이미 ACTIVE인 기존 회원에게 사용한다.
     *
     * @param tokens 발급된 정식 토큰 쌍
     * @return onboarding=false, onboardingToken=null인 결과
     */
    public static AuthResult login(TokenPair tokens) {
        return new AuthResult(false, null, tokens);
    }
}
