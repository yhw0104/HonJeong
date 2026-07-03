package com.honjeong.checkin.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.honjeong.checkin.dto.CheckInRequest;
import com.honjeong.checkin.dto.CheckInResponse;
import com.honjeong.checkin.dto.CheckInStatsResponse;
import com.honjeong.checkin.dto.MapMarkerResponse;
import com.honjeong.checkin.service.CheckInService;
import com.honjeong.global.common.ApiResponse;
import com.honjeong.global.security.CurrentUserId;

import jakarta.validation.Valid;

/**
 * 체크인 REST 컨트롤러(/api/check-ins). 얇게 유지 — {@code @CurrentUserId}·{@code @Valid}·DTO 변환만 하고
 * 검증·집계·매핑은 {@link CheckInService}에 위임한다.
 *
 * <p><b>인가:</b> 모든 경로가 정식 USER 전용이다. SecurityConfig의 {@code anyRequest().hasRole("USER")} 기본 규칙이
 * 커버하므로 별도 매처가 필요 없다(토큰 없으면 401, 온보딩 토큰이면 403).
 */
@RestController
@RequestMapping("/api/check-ins")
public class CheckInController {

    private final CheckInService checkInService;

    public CheckInController(CheckInService checkInService) {
        this.checkInService = checkInService;
    }

    /** 혼밥 체크인 시작. 같은 장소 멱등 재요청도 201로 통일한다. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CheckInResponse> create(@CurrentUserId Long userId,
            @Valid @RequestBody CheckInRequest request) {
        return ApiResponse.success(checkInService.createCheckIn(userId, request));
    }

    /** 체크인 종료. */
    @PatchMapping("/{id}/end")
    public ApiResponse<CheckInResponse> end(@CurrentUserId Long userId, @PathVariable Long id) {
        return ApiResponse.success(checkInService.endCheckIn(userId, id));
    }

    /** 짧은 혼밥 취소(오집계 제외). 소유자의 ACTIVE만. */
    @PatchMapping("/{id}/cancel")
    public ApiResponse<CheckInResponse> cancel(@CurrentUserId Long userId, @PathVariable Long id) {
        return ApiResponse.success(checkInService.cancelCheckIn(userId, id));
    }

    /** 내 현재 ACTIVE 체크인(없으면 data:null). */
    @GetMapping("/me")
    public ApiResponse<CheckInResponse> me(@CurrentUserId Long userId) {
        return ApiResponse.success(checkInService.getMyActiveCheckIn(userId));
    }

    /** 사회적 증거 통계("오늘 N명 / 현재 N명"). */
    @GetMapping("/stats")
    public ApiResponse<CheckInStatsResponse> stats() {
        return ApiResponse.success(checkInService.getStats());
    }

    /** 반경 내 식당별 현재 혼밥러 수 마커. lat/lng 필수, radius 기본 1000m. */
    @GetMapping("/map")
    public ApiResponse<List<MapMarkerResponse>> map(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(defaultValue = "1000") int radius) {
        return ApiResponse.success(checkInService.getMap(lat, lng, radius));
    }
}
