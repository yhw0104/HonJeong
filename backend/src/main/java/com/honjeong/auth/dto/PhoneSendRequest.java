package com.honjeong.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 인증번호 발송 요청 본문. {@code POST /api/auth/phone/send-code}에서 받는다.
 *
 * @param phone 인증번호를 받을 휴대폰번호. {@code @NotBlank}이므로 비어 있으면 검증 실패해 400으로 응답된다.
 */
public record PhoneSendRequest(@NotBlank String phone) {
}
