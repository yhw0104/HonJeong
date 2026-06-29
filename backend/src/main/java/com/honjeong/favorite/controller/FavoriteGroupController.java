package com.honjeong.favorite.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.honjeong.favorite.dto.AddFavoriteRequest;
import com.honjeong.favorite.dto.CreateGroupRequest;
import com.honjeong.favorite.dto.FavoriteGroupDetailResponse;
import com.honjeong.favorite.dto.FavoriteGroupSummaryResponse;
import com.honjeong.favorite.dto.UpdateGroupRequest;
import com.honjeong.favorite.service.FavoriteGroupService;
import com.honjeong.favorite.service.FavoriteService;
import com.honjeong.global.common.ApiResponse;
import com.honjeong.global.security.CurrentUserId;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/favorite-groups")
public class FavoriteGroupController {

    private final FavoriteGroupService groupService;
    private final FavoriteService favoriteService;

    public FavoriteGroupController(FavoriteGroupService groupService, FavoriteService favoriteService) {
        this.groupService = groupService;
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public ApiResponse<List<FavoriteGroupSummaryResponse>> list(@CurrentUserId Long userId) {
        return ApiResponse.success(groupService.getGroups(userId));
    }

    @GetMapping("/{groupId}")
    public ApiResponse<FavoriteGroupDetailResponse> detail(@CurrentUserId Long userId,
            @PathVariable Long groupId) {
        return ApiResponse.success(groupService.getGroupDetail(userId, groupId));
    }

    @PostMapping
    public ApiResponse<FavoriteGroupSummaryResponse> create(@CurrentUserId Long userId,
            @Valid @RequestBody CreateGroupRequest request) {
        return ApiResponse.success(groupService.createGroup(userId, request));
    }

    @PatchMapping("/{groupId}")
    public ApiResponse<FavoriteGroupSummaryResponse> update(@CurrentUserId Long userId,
            @PathVariable Long groupId, @Valid @RequestBody UpdateGroupRequest request) {
        return ApiResponse.success(groupService.updateGroup(userId, groupId, request));
    }

    @DeleteMapping("/{groupId}")
    public ApiResponse<Void> delete(@CurrentUserId Long userId, @PathVariable Long groupId) {
        groupService.deleteGroup(userId, groupId);
        return ApiResponse.success(null);
    }

    @PostMapping("/{groupId}/places")
    public ApiResponse<Void> addPlace(@CurrentUserId Long userId, @PathVariable Long groupId,
            @Valid @RequestBody AddFavoriteRequest request) {
        favoriteService.addPlace(userId, groupId, request.placeId());
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{groupId}/places/{placeId}")
    public ApiResponse<Void> removePlace(@CurrentUserId Long userId, @PathVariable Long groupId,
            @PathVariable Long placeId) {
        favoriteService.removePlace(userId, groupId, placeId);
        return ApiResponse.success(null);
    }
}
