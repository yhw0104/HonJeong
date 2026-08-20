package com.honjeong.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 소셜 로그인 요청 본문. {@code POST /api/auth/oauth/{provider}}에서 받는다.
 *
 * @param idToken 소셜 공급자(카카오/애플)가 발급한 ID 토큰. 서버가 이 토큰을 검증해 공급자 사용자 식별값을 얻는다.
 *                {@code @NotBlank}이므로 null·빈 문자열·공백뿐이면 검증 실패해 400으로 응답된다.
 * @param authorizationCode 애플만 보내는 단기 인가 코드(nullable). 탈퇴 시 애플 토큰을 폐기하려면
 *                          이 값을 refresh token으로 교환해 두어야 한다. 카카오는 보내지 않으며,
 *                          없어도 로그인은 정상 동작한다.
 */
public record OAuthLoginRequest(@NotBlank String idToken, String authorizationCode) {
}
