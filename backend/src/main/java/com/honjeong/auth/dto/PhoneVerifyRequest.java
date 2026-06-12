package com.honjeong.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 인증번호 확인 요청 본문. {@code POST /api/auth/phone/verify}에서 받는다.
 *
 * @param phone 인증번호를 받았던 휴대폰번호. {@code @NotBlank}.
 * @param code  사용자가 입력한 인증번호(개발 모드 기준 {@code 000000}). {@code @NotBlank}. 둘 중 하나라도 비면 400.
 */
public record PhoneVerifyRequest(@NotBlank String phone, @NotBlank String code) {
}
