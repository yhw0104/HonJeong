package com.honjeong.push.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.honjeong.global.common.ApiResponse;
import com.honjeong.global.security.CurrentUserId;
import com.honjeong.push.dto.DeviceTokenDeleteRequest;
import com.honjeong.push.dto.DeviceTokenRequest;
import com.honjeong.push.service.DeviceTokenService;

import jakarta.validation.Valid;

/**
 * 푸시 기기 토큰 등록·해제 API.
 *
 * <p>기본 경로: /api/device-tokens
 *
 * <p>둘 다 정식 USER 전용이다(SecurityConfig 기본 규칙 — 공개 경로에 추가하지 않는다).
 */
@RestController
@RequestMapping("/api/device-tokens")
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    public DeviceTokenController(DeviceTokenService deviceTokenService) {
        this.deviceTokenService = deviceTokenService;
    }

    /**
     * 기기 토큰을 등록한다(이미 있는 토큰이면 주인을 갱신한다).
     *
     * <p>사용 화면: 권한 허용 직후·앱 시작 시·토큰 갱신 시.
     *
     * @param userId  인증 사용자 ID
     * @param request 토큰·플랫폼
     * @return 빈 성공 응답
     */
    @PostMapping
    public ApiResponse<Void> register(@CurrentUserId Long userId,
            @Valid @RequestBody DeviceTokenRequest request) {
        deviceTokenService.register(userId, request.token(), request.platform());
        return ApiResponse.success(null);
    }

    /**
     * 기기 토큰을 해제한다(로그아웃).
     *
     * <p>토큰을 URL이 아니라 본문에 담는 것은 의도적이다 — 경로에 넣으면 접근 로그에 남는다.
     *
     * @param userId  인증 사용자 ID
     * @param request 해제할 토큰
     * @return 빈 성공 응답
     */
    @DeleteMapping
    public ApiResponse<Void> unregister(@CurrentUserId Long userId,
            @Valid @RequestBody DeviceTokenDeleteRequest request) {
        deviceTokenService.unregister(userId, request.token());
        return ApiResponse.success(null);
    }
}
