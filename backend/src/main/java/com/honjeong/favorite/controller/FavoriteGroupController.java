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
     * 내 즐겨찾기 그룹 목록을 조회한다.
     *
     * <p>사용 화면: 즐겨찾기(Favorites)의 내 그룹 목록.
     *
     * @param userId 인증 사용자 ID
     * @return 그룹 ID, 이름, 메모, 색상, 기본 그룹 여부, 담긴 장소 수
     */
    @GetMapping
    public ApiResponse<List<FavoriteGroupSummaryResponse>> list(@CurrentUserId Long userId) {
        return ApiResponse.success(groupService.getGroups(userId));
    }

    /**
     * 그룹 상세(담긴 장소 목록)를 조회한다.
     *
     * <p>사용 화면: 즐겨찾기(Favorites)에서 그룹을 펼쳤을 때.
     *
     * @param userId 인증 사용자 ID
     * @param groupId 조회할 그룹 ID
     * @return 그룹 정보 + 담긴 장소 목록(방문 여부 포함)
     */
    @GetMapping("/{groupId}")
    public ApiResponse<FavoriteGroupDetailResponse> detail(@CurrentUserId Long userId,
            @PathVariable Long groupId) {
        return ApiResponse.success(groupService.getGroupDetail(userId, groupId));
    }

    /**
     * 새 즐겨찾기 그룹을 만든다.
     *
     * <p>사용 화면: 새 그룹 만들기(NewGroup), 즐겨찾기 저장 시트(FavoriteSheet)에서의 즉시 생성.
     *
     * @param userId 인증 사용자 ID
     * @param request name(필수), note, color
     * @return 생성된 그룹 요약(장소 수 0)
     */
    @PostMapping
    public ApiResponse<FavoriteGroupSummaryResponse> create(@CurrentUserId Long userId,
            @Valid @RequestBody CreateGroupRequest request) {
        return ApiResponse.success(groupService.createGroup(userId, request));
    }

    /**
     * 그룹의 이름·메모·색상을 부분 수정한다.
     *
     * <p>사용 화면: 새 그룹 만들기(NewGroup)의 편집 모드.
     *
     * @param userId 인증 사용자 ID
     * @param groupId 수정할 그룹 ID
     * @param request name, note, color(모두 선택 — null이면 미변경)
     * @return 수정이 반영된 그룹 요약
     */
    @PatchMapping("/{groupId}")
    public ApiResponse<FavoriteGroupSummaryResponse> update(@CurrentUserId Long userId,
            @PathVariable Long groupId, @Valid @RequestBody UpdateGroupRequest request) {
        return ApiResponse.success(groupService.updateGroup(userId, groupId, request));
    }

    /**
     * 그룹과 그 안의 즐겨찾기를 함께 삭제한다. 기본 그룹은 삭제할 수 없다.
     *
     * <p>사용 화면: 즐겨찾기(Favorites)의 그룹 삭제.
     *
     * @param userId 인증 사용자 ID
     * @param groupId 삭제할 그룹 ID
     * @return 본문 데이터 없음 — 성공 여부만 응답 엔벨로프로 전달
     */
    @DeleteMapping("/{groupId}")
    public ApiResponse<Void> delete(@CurrentUserId Long userId, @PathVariable Long groupId) {
        groupService.deleteGroup(userId, groupId);
        return ApiResponse.success(null);
    }

    /**
     * 그룹에 장소를 담는다. 이미 담겨 있으면 아무것도 하지 않는다(멱등).
     *
     * <p>사용 화면: 즐겨찾기 저장 시트(FavoriteSheet) — 식당 상세(RestaurantDetail)에서 진입.
     *
     * @param userId 인증 사용자 ID
     * @param groupId 담을 그룹 ID
     * @param request placeId(필수)
     * @return 본문 데이터 없음 — 성공 여부만 응답 엔벨로프로 전달
     */
    @PostMapping("/{groupId}/places")
    public ApiResponse<Void> addPlace(@CurrentUserId Long userId, @PathVariable Long groupId,
            @Valid @RequestBody AddFavoriteRequest request) {
        favoriteService.addPlace(userId, groupId, request.placeId());
        return ApiResponse.success(null);
    }

    /**
     * 그룹에서 장소를 뺀다.
     *
     * <p>사용 화면: 즐겨찾기 저장 시트(FavoriteSheet), 즐겨찾기(Favorites)의 그룹 상세.
     *
     * @param userId 인증 사용자 ID
     * @param groupId 대상 그룹 ID
     * @param placeId 뺄 장소 ID
     * @return 본문 데이터 없음 — 성공 여부만 응답 엔벨로프로 전달
     */
    @DeleteMapping("/{groupId}/places/{placeId}")
    public ApiResponse<Void> removePlace(@CurrentUserId Long userId, @PathVariable Long groupId,
            @PathVariable Long placeId) {
        favoriteService.removePlace(userId, groupId, placeId);
        return ApiResponse.success(null);
    }
}
