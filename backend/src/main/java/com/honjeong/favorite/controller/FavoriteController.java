package com.honjeong.favorite.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.honjeong.favorite.dto.FavoriteStatusResponse;
import com.honjeong.favorite.service.FavoriteService;
import com.honjeong.global.common.ApiResponse;
import com.honjeong.global.security.CurrentUserId;

/**
 * 장소 기준 즐겨찾기 상태 조회 컨트롤러.
 *
 * <p>기본 경로: /api/places
 */
@RestController
@RequestMapping("/api/places")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    /**
     * 1. API 주소: GET /api/places/{placeId}/favorite-status
     * 2. 사용 화면: 식당 상세(RestaurantDetail) — 즐겨찾기 버튼 상태 표시 / 즐겨찾기 저장 시트(FavoriteSheet) — 그룹별 담김 여부 체크 표시
     * 3. Request: placeId(경로) — 조회할 장소 ID / 인증 사용자(@CurrentUserId)
     * 4. Response: FavoriteStatusResponse — 즐겨찾기 여부(saved), 내 그룹 목록과 각 그룹의 포함 여부
     */
    @GetMapping("/{placeId}/favorite-status")
    public ApiResponse<FavoriteStatusResponse> status(@CurrentUserId Long userId,
            @PathVariable Long placeId) {
        return ApiResponse.success(favoriteService.getStatus(userId, placeId));
    }
}
