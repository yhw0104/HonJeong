package com.honjeong.auth.service;

/**
 * 발급된 토큰 묶음을 담아 클라이언트로 전달하는 불변 값 객체(record). 로그인·재발급 시 {@link TokenService}가
 * 만들어 돌려준다.
 *
 * @param accessToken      access JWT(짧은 수명). 매 API 요청에 실어 보내 신원을 증명한다.
 * @param refreshToken     refresh 토큰 <b>원문</b>(긴 수명). 서버엔 해시만 저장되고 원문은 이 값으로만 전달된다.
 * @param expiresInSeconds access 토큰의 유효기간(초). 클라가 만료 시점을 알고 미리 재발급할 수 있게 한다.
 */
public record TokenPair(String accessToken, String refreshToken, long expiresInSeconds) {
}
