package com.honjeong.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml의 {@code honjeong.jwt.*} 값을 바인딩하는 타입 안전 설정 프로퍼티.
 * 서명용 시크릿과 토큰 종류별 만료 시간(초)을 담는다.
 *
 * @param secret HS256 서명에 쓰는 대칭키 문자열({@link com.honjeong.global.security.JwtProvider}가 HmacSHA256 키로 사용)
 * @param accessTokenTtlSeconds 액세스 토큰의 유효기간(초)
 * @param refreshTokenTtlSeconds 리프레시 토큰의 유효기간(초) — refresh는 JWT가 아니라 DB 저장 불투명 토큰이라 토큰 서비스 쪽에서 사용
 * @param onboardingTokenTtlSeconds 온보딩 임시 토큰의 유효기간(초)
 */
@ConfigurationProperties("honjeong.jwt")
public record HonjeongJwtProperties(
        String secret,
        long accessTokenTtlSeconds,
        long refreshTokenTtlSeconds,
        long onboardingTokenTtlSeconds) {
}
