package com.honjeong.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 리프레시 토큰 요청 본문. 토큰 재발급({@code /refresh})과 로그아웃({@code /logout}) 두 엔드포인트가 공용으로 쓴다.
 *
 * @param refreshToken 회전(재발급)하거나 무효화(로그아웃)할 리프레시 토큰 원문. {@code @NotBlank}이므로 비면 400.
 */
public record RefreshRequest(@NotBlank String refreshToken) {
}
