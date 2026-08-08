package com.honjeong.push.dto;

import com.honjeong.push.domain.Platform;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 기기 토큰 등록 본문.
 *
 * @param token          FCM 등록 토큰. DB 컬럼이 VARCHAR(255)라 같은 상한을 건다 —
 *                       상한이 없으면 긴 값이 DB 제약 위반까지 내려가 500으로 나간다.
 * @param platform       기기 플랫폼
 * @param installationId 앱 설치 식별자(선택). 있으면 같은 기기의 옛 토큰을 정리한다.
 *                       <b>선택인 이유</b>: 서버가 앱보다 먼저 배포되므로 한동안 이 값을 보내지 않는
 *                       구버전 앱이 계속 등록한다. 필수로 걸면 그 등록이 400으로 죽어 푸시가 끊긴다.
 */
public record DeviceTokenRequest(
        @NotBlank @Size(max = 255) String token,
        @NotNull Platform platform,
        @Size(max = 64) String installationId) {
}
