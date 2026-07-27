package com.honjeong.mate.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.honjeong.global.common.ApiResponse;
import com.honjeong.global.security.CurrentUserId;
import com.honjeong.mate.dto.PlaceMatesResponse;
import com.honjeong.mate.service.PlaceMateService;

/** 식당 상세 메이트 탭 — 내 메이트 × 이 식당(인증 필요). */
@RestController
@RequestMapping("/api/places")
public class PlaceMateController {

    private final PlaceMateService placeMateService;

    public PlaceMateController(PlaceMateService placeMateService) {
        this.placeMateService = placeMateService;
    }

    /**
     * 1. API 주소: GET /api/places/{placeId}/mates
     * 2. 사용 화면: 식당 상세(RestaurantDetail) 메이트 탭
     * 3. Request: placeId(경로) + 인증 사용자(@CurrentUserId)
     * 4. Response: PlaceMatesResponse — visitedCount + 메이트 목록(모집중 우선)
     */
    @GetMapping("/{placeId}/mates")
    public ApiResponse<PlaceMatesResponse> matesAtPlace(@CurrentUserId Long userId,
            @PathVariable Long placeId) {
        return ApiResponse.success(placeMateService.getMatesAtPlace(userId, placeId));
    }
}
