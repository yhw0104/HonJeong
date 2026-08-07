package com.honjeong.push.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 기기 토큰 해제 본문.
 *
 * @param token 해제할 FCM 등록 토큰
 */
public record DeviceTokenDeleteRequest(@NotBlank @Size(max = 255) String token) {
}
