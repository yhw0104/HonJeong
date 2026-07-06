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

/**
 * 즐겨찾기 그룹 CRUD 및 그룹 내 장소 추가/삭제 컨트롤러.
 *
 * <p>기본 경로: /api/favorite-groups
 */
@RestController
@RequestMapping("/api/favorite-groups")
public class FavoriteGroupController {

    private final FavoriteGroupService groupService;
    private final FavoriteService favoriteService;

    public FavoriteGroupController(FavoriteGroupService groupService, FavoriteService favoriteService) {
        this.groupService = groupService;
        this.favoriteService = favoriteService;
    }

    /**
     * 1. API 주소: GET /api/favorite-groups
     * 2. 사용 화면: 즐겨찾기(Favorites) — 내 그룹 목록 표시
     * 3. Request: 인증 사용자(@CurrentUserId)
     * 4. Response: List&lt;FavoriteGroupSummaryResponse&gt; — 그룹 ID, 이름, 메모, 색상, 기본 그룹 여부, 담긴 장소 수
     */
    @GetMapping
    public ApiResponse<List<FavoriteGroupSummaryResponse>> list(@CurrentUserId Long userId) {
        return ApiResponse.success(groupService.getGroups(userId));
    }

    /**
     * 1. API 주소: GET /api/favorite-groups/{groupId}
     * 2. 사용 화면: 즐겨찾기(Favorites) — 그룹 펼침 시 담긴 장소 목록 표시
     * 3. Request: groupId(경로) — 조회할 그룹 ID / 인증 사용자(@CurrentUserId)
     * 4. Response: FavoriteGroupDetailResponse — 그룹 정보 + 담긴 장소 목록(방문 여부 포함)
     */
    @GetMapping("/{groupId}")
    public ApiResponse<FavoriteGroupDetailResponse> detail(@CurrentUserId Long userId,
            @PathVariable Long groupId) {
        return ApiResponse.success(groupService.getGroupDetail(userId, groupId));
    }

    /**
     * 1. API 주소: POST /api/favorite-groups
     * 2. 사용 화면: 새 그룹 만들기(NewGroup) — 그룹 생성 / 즐겨찾기 저장 시트(FavoriteSheet) — 시트 안에서 새 그룹 즉시 생성
     * 3. Request: CreateGroupRequest(바디) — name(필수), note, color / 인증 사용자(@CurrentUserId)
     * 4. Response: FavoriteGroupSummaryResponse — 생성된 그룹 요약(장소 수 0)
     */
    @PostMapping
    public ApiResponse<FavoriteGroupSummaryResponse> create(@CurrentUserId Long userId,
            @Valid @RequestBody CreateGroupRequest request) {
        return ApiResponse.success(groupService.createGroup(userId, request));
    }

    /**
     * 1. API 주소: PATCH /api/favorite-groups/{groupId}
     * 2. 사용 화면: 새 그룹 만들기(NewGroup) — 편집 모드에서 그룹 이름/메모/색상 수정
     * 3. Request: groupId(경로) — 수정할 그룹 ID / UpdateGroupRequest(바디) — name, note, color(모두 선택, null이면 미변경) / 인증 사용자(@CurrentUserId)
     * 4. Response: FavoriteGroupSummaryResponse — 수정 반영된 그룹 요약
     */
    @PatchMapping("/{groupId}")
    public ApiResponse<FavoriteGroupSummaryResponse> update(@CurrentUserId Long userId,
            @PathVariable Long groupId, @Valid @RequestBody UpdateGroupRequest request) {
        return ApiResponse.success(groupService.updateGroup(userId, groupId, request));
    }

    /**
     * 1. API 주소: DELETE /api/favorite-groups/{groupId}
     * 2. 사용 화면: 즐겨찾기(Favorites) — 그룹 삭제(기본 그룹은 삭제 불가)
     * 3. Request: groupId(경로) — 삭제할 그룹 ID / 인증 사용자(@CurrentUserId)
     * 4. Response: 없음(Void) — 성공 봉투만 반환
     */
    @DeleteMapping("/{groupId}")
    public ApiResponse<Void> delete(@CurrentUserId Long userId, @PathVariable Long groupId) {
        groupService.deleteGroup(userId, groupId);
        return ApiResponse.success(null);
    }

    /**
     * 1. API 주소: POST /api/favorite-groups/{groupId}/places
     * 2. 사용 화면: 즐겨찾기 저장 시트(FavoriteSheet) — 식당 상세(RestaurantDetail)에서 그룹에 장소 담기
     * 3. Request: groupId(경로) — 담을 그룹 ID / AddFavoriteRequest(바디) — placeId(필수) / 인증 사용자(@CurrentUserId)
     * 4. Response: 없음(Void) — 성공 봉투만 반환(이미 담긴 경우 멱등 처리)
     */
    @PostMapping("/{groupId}/places")
    public ApiResponse<Void> addPlace(@CurrentUserId Long userId, @PathVariable Long groupId,
            @Valid @RequestBody AddFavoriteRequest request) {
        favoriteService.addPlace(userId, groupId, request.placeId());
        return ApiResponse.success(null);
    }

    /**
     * 1. API 주소: DELETE /api/favorite-groups/{groupId}/places/{placeId}
     * 2. 사용 화면: 즐겨찾기 저장 시트(FavoriteSheet) — 그룹에서 장소 빼기 / 즐겨찾기(Favorites) — 그룹 상세에서 장소 삭제
     * 3. Request: groupId(경로) — 대상 그룹 ID, placeId(경로) — 뺄 장소 ID / 인증 사용자(@CurrentUserId)
     * 4. Response: 없음(Void) — 성공 봉투만 반환
     */
    @DeleteMapping("/{groupId}/places/{placeId}")
    public ApiResponse<Void> removePlace(@CurrentUserId Long userId, @PathVariable Long groupId,
            @PathVariable Long placeId) {
        favoriteService.removePlace(userId, groupId, placeId);
        return ApiResponse.success(null);
    }
}
