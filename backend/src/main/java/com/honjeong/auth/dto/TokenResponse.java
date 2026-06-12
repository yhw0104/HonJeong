package com.honjeong.auth.dto;

import com.honjeong.auth.service.TokenPair;

/**
 * 토큰 발급 응답 본문. 프로필 완료({@code /complete})와 토큰 재발급({@code /refresh})이 이 형태로 응답한다.
 *
 * @param accessToken  API 호출에 쓰는 액세스 토큰.
 * @param refreshToken 액세스 토큰 재발급에 쓰는 리프레시 토큰.
 * @param expiresIn    액세스 토큰 만료까지 남은 시간(초).
 */
public record TokenResponse(String accessToken, String refreshToken, long expiresIn) {

    /**
     * 서비스 계층의 {@link TokenPair}를 응답 DTO로 변환한다.
     *
     * <p>{@code TokenPair}의 access/refresh 토큰과 {@code expiresInSeconds}를 그대로 옮겨 담는 단순 매핑이다
     * (만료 시간 필드명만 {@code expiresInSeconds} → {@code expiresIn}으로 바뀐다).
     */
    public static TokenResponse from(TokenPair pair) {
        return new TokenResponse(pair.accessToken(), pair.refreshToken(), pair.expiresInSeconds());
    }
}
