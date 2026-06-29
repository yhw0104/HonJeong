package com.honjeong.favorite.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.honjeong.favorite.dto.FavoriteStatusResponse;
import com.honjeong.favorite.service.FavoriteService;
import com.honjeong.global.common.ApiResponse;
import com.honjeong.global.security.CurrentUserId;

@RestController
@RequestMapping("/api/places")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping("/{placeId}/favorite-status")
    public ApiResponse<FavoriteStatusResponse> status(@CurrentUserId Long userId,
            @PathVariable Long placeId) {
        return ApiResponse.success(favoriteService.getStatus(userId, placeId));
    }
}
