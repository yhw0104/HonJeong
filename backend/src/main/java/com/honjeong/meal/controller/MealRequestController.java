package com.honjeong.meal.controller;

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

import com.honjeong.global.common.ApiResponse;
import com.honjeong.global.security.CurrentUserId;
import com.honjeong.meal.dto.MealRequestCreateRequest;
import com.honjeong.meal.dto.MealRequestListItemResponse;
import com.honjeong.meal.dto.MealRequestResponse;
import com.honjeong.meal.dto.MealRequestStatusResponse;
import com.honjeong.meal.service.MealRequestService;

import jakarta.validation.Valid;

/**
 * 같이먹기 신청 REST 컨트롤러(/api/meal-requests). 얇게 유지 — {@code @CurrentUserId}·{@code @Valid}·DTO 변환만 하고
 * 검증·매핑은 {@link MealRequestService}에 위임한다.
 *
 * <p><b>인가:</b> 모든 경로가 정식 USER 전용이다. SecurityConfig의 {@code anyRequest().hasRole("USER")} 기본 규칙이
 * 커버하므로 별도 매처가 필요 없다(토큰 없으면 401, 온보딩 토큰이면 403).
 */
@RestController
@RequestMapping("/api/meal-requests")
public class MealRequestController {

    private final MealRequestService mealRequestService;

    public MealRequestController(MealRequestService mealRequestService) {
        this.mealRequestService = mealRequestService;
    }

    /** 같이먹기 신청. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MealRequestResponse> create(@CurrentUserId Long userId,
            @Valid @RequestBody MealRequestCreateRequest request) {
        return ApiResponse.success(mealRequestService.create(userId, request));
    }

    /** 받은/보낸 신청 목록. role 기본 received, status 선택 필터. */
    @GetMapping
    public ApiResponse<List<MealRequestListItemResponse>> list(@CurrentUserId Long userId,
            @RequestParam(defaultValue = "received") String role,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(mealRequestService.getMealRequests(userId, role, status));
    }

    /** 신청 수락. */
    @PatchMapping("/{id}/accept")
    public ApiResponse<MealRequestStatusResponse> accept(@CurrentUserId Long userId, @PathVariable Long id) {
        return ApiResponse.success(mealRequestService.accept(userId, id));
    }

    /** 신청 거절. */
    @PatchMapping("/{id}/decline")
    public ApiResponse<MealRequestStatusResponse> decline(@CurrentUserId Long userId, @PathVariable Long id) {
        return ApiResponse.success(mealRequestService.decline(userId, id));
    }
}
