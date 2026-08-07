package com.honjeong.push.dto;

import com.honjeong.push.domain.Platform;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 기기 토큰 등록 본문.
 *
 * @param token    FCM 등록 토큰. DB 컬럼이 VARCHAR(255)라 같은 상한을 건다 —
 *                 상한이 없으면 긴 값이 DB 제약 위반까지 내려가 500으로 나간다.
 * @param platform 기기 플랫폼
 */
public record DeviceTokenRequest(
        @NotBlank @Size(max = 255) String token,
        @NotNull Platform platform) {
}
