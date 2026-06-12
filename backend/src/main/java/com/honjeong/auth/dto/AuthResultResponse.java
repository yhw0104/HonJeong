package com.honjeong.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.honjeong.auth.service.AuthResult;
import com.honjeong.auth.service.TokenPair;

/**
 * 인증 진입(소셜 {@code /oauth/{provider}}·휴대폰 {@code /phone/verify}) 응답 본문.
 *
 * <p>이 응답은 두 가지 모습을 한 record로 표현한다 — 신규(온보딩 필요)면 {@code onboarding=true}와 {@code onboardingToken}만,
 * 기존 ACTIVE 회원이면 {@code onboarding=false}와 토큰 3종(access/refresh/expiresIn)만 의미가 있다.
 *
 * <p>클래스에 붙은 {@code @JsonInclude(NON_NULL)} 덕분에 값이 {@code null}인 필드는 JSON 직렬화에서 제외된다.
 * 그래서 신규 응답에는 토큰 필드들이 아예 나타나지 않고, 기존 응답에는 {@code onboardingToken}이 나타나지 않는다 —
 * 즉 상황에 맞는 필드만 깔끔하게 내려간다. 클라이언트는 {@code onboarding} 불리언으로 두 경우를 분기하면 된다.
 *
 * @param onboarding     신규/미완 회원이라 온보딩이 필요한지 여부({@code true}면 아래 토큰 3종 대신 {@code onboardingToken}을 본다).
 * @param onboardingToken 온보딩 단계({@code /terms}, {@code /complete})에서 쓰는 임시 토큰. 기존 회원이면 null이라 생략된다.
 * @param accessToken    로그인 성공 시의 액세스 토큰. 신규(온보딩)면 null이라 생략된다.
 * @param refreshToken   로그인 성공 시의 리프레시 토큰. 신규(온보딩)면 null이라 생략된다.
 * @param expiresIn      액세스 토큰 만료까지 남은 시간(초). 신규(온보딩)면 null이라 생략된다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResultResponse(
        boolean onboarding,
        String onboardingToken,
        String accessToken,
        String refreshToken,
        Long expiresIn) {

    /**
     * 서비스 계층의 {@link AuthResult}를 응답 DTO로 변환한다.
     *
     * <p>{@code result.onboarding()} 결과로 분기한다:
     * <ul>
     *   <li><b>신규(온보딩):</b> {@code onboarding=true}, {@code onboardingToken}만 채우고 토큰 3종은 모두 null로 둔다
     *       (→ NON_NULL 규칙에 따라 직렬화에서 빠진다).</li>
     *   <li><b>기존(로그인):</b> {@code onboarding=false}, {@code onboardingToken}은 null로 두고 {@code result.tokens()}의
     *       access/refresh/만료(초)를 채운다.</li>
     * </ul>
     */
    public static AuthResultResponse from(AuthResult result) {
        if (result.onboarding()) {
            return new AuthResultResponse(true, result.onboardingToken(), null, null, null);
        }
        TokenPair tokens = result.tokens();
        return new AuthResultResponse(false, null, tokens.accessToken(), tokens.refreshToken(),
                tokens.expiresInSeconds());
    }
}
