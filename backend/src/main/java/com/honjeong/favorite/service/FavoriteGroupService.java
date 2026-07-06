package com.honjeong.favorite.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.honjeong.checkin.repository.CheckInRepository;
import com.honjeong.favorite.domain.Favorite;
import com.honjeong.favorite.domain.FavoriteGroup;
import com.honjeong.favorite.dto.CreateGroupRequest;
import com.honjeong.favorite.dto.FavoriteGroupDetailResponse;
import com.honjeong.favorite.dto.FavoriteGroupSummaryResponse;
import com.honjeong.favorite.dto.FavoritePlaceResponse;
import com.honjeong.favorite.dto.UpdateGroupRequest;
import com.honjeong.favorite.repository.FavoriteGroupRepository;
import com.honjeong.favorite.repository.FavoriteRepository;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.place.domain.Place;
import com.honjeong.user.domain.User;
import com.honjeong.user.repository.UserRepository;

/**
 * 1. 기능: 즐겨찾기 그룹의 생성·조회·수정·삭제 및 가입 시 기본 그룹("기본 그룹") 자동 생성 비즈니스 로직
 * 2. 사용 Controller: FavoriteGroupController (createDefaultGroup은 AuthService에서 호출)
 */
@Service
public class FavoriteGroupService {

    private static final String DEFAULT_COLOR = "#FF5A1F";

    private final FavoriteGroupRepository groupRepository;
    private final FavoriteRepository favoriteRepository;
    private final CheckInRepository checkInRepository;
    private final UserRepository userRepository;

    public FavoriteGroupService(FavoriteGroupRepository groupRepository, FavoriteRepository favoriteRepository,
            CheckInRepository checkInRepository, UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.favoriteRepository = favoriteRepository;
        this.checkInRepository = checkInRepository;
        this.userRepository = userRepository;
    }

    /**
     * 기능: 사용자의 그룹 목록을 담긴 장소 수 포함 요약으로 조회(생성 순 정렬)
     * Request: userId — 요청 사용자 ID
     * Response: List&lt;FavoriteGroupSummaryResponse&gt; — 그룹 요약 목록(장소 수 포함)
     */
    @Transactional(readOnly = true)
    public List<FavoriteGroupSummaryResponse> getGroups(Long userId) {
        return groupRepository.findSummaries(userId);
    }

    /**
     * 기능: 소유 검증 후 그룹 상세(담긴 장소 목록 + 각 장소의 체크인 기반 방문 여부)를 조회
     * Request: userId — 요청 사용자 ID, groupId — 조회할 그룹 ID
     * Response: FavoriteGroupDetailResponse — 그룹 정보 + 장소 목록(visited 포함)
     */
    @Transactional(readOnly = true)
    public FavoriteGroupDetailResponse getGroupDetail(Long userId, Long groupId) {
        FavoriteGroup group = loadOwned(userId, groupId);
        List<Favorite> favorites = favoriteRepository.findWithPlaceByGroupId(groupId);
        List<Long> placeIds = favorites.stream().map(f -> f.getPlace().getId()).toList();
        Set<Long> visited = placeIds.isEmpty()
                ? Set.of()
                : Set.copyOf(checkInRepository.findVisitedPlaceIds(userId, placeIds));
        List<FavoritePlaceResponse> places = favorites.stream()
                .map(f -> toPlaceResponse(f.getPlace(), visited.contains(f.getPlace().getId())))
                .toList();
        return new FavoriteGroupDetailResponse(group.getId(), group.getName(), group.getNote(),
                group.getColor(), group.isDefault(), places);
    }

    /**
     * 기능: 새 일반 그룹 생성(색상 미지정 시 기본 색 #FF5A1F 적용, isDefault=false)
     * Request: userId — 요청 사용자 ID, req — CreateGroupRequest(name·note·color)
     * Response: FavoriteGroupSummaryResponse — 생성된 그룹 요약(장소 수 0)
     */
    @Transactional
    public FavoriteGroupSummaryResponse createGroup(Long userId, CreateGroupRequest req) {
        User userRef = userRepository.getReferenceById(userId);
        String color = (req.color() == null || req.color().isBlank()) ? DEFAULT_COLOR : req.color();
        FavoriteGroup group = groupRepository.save(
                FavoriteGroup.create(userRef, req.name(), req.note(), color, false));
        return new FavoriteGroupSummaryResponse(group.getId(), group.getName(), group.getNote(),
                group.getColor(), group.isDefault(), 0L);
    }

    /**
     * 기능: 소유 검증 후 그룹 이름/메모/색상을 부분 수정(null 필드는 미변경)
     * Request: userId — 요청 사용자 ID, groupId — 수정할 그룹 ID, req — UpdateGroupRequest(name·note·color)
     * Response: FavoriteGroupSummaryResponse — 수정 반영된 그룹 요약(현재 장소 수 포함)
     */
    @Transactional
    public FavoriteGroupSummaryResponse updateGroup(Long userId, Long groupId, UpdateGroupRequest req) {
        FavoriteGroup group = loadOwned(userId, groupId);
        group.updateInfo(req.name(), req.note(), req.color());
        long count = favoriteRepository.countByGroup_Id(groupId);
        return new FavoriteGroupSummaryResponse(group.getId(), group.getName(), group.getNote(),
                group.getColor(), group.isDefault(), count);
    }

    /**
     * 기능: 소유 검증 후 그룹과 그 안의 즐겨찾기를 함께 삭제(기본 그룹이면 DEFAULT_GROUP_NOT_DELETABLE 예외)
     * Request: userId — 요청 사용자 ID, groupId — 삭제할 그룹 ID
     * Response: 없음(void)
     */
    @Transactional
    public void deleteGroup(Long userId, Long groupId) {
        FavoriteGroup group = loadOwned(userId, groupId);
        if (group.isDefault()) {
            throw new BusinessException(ErrorCode.DEFAULT_GROUP_NOT_DELETABLE);
        }
        favoriteRepository.deleteByGroup_Id(groupId);
        groupRepository.delete(group);
    }

    /**
     * 기능: 가입(첫 로그인) 시 기본 그룹 "기본 그룹" 자동 생성(이미 있으면 아무것도 안 함 — 멱등)
     * Request: userId — 대상 사용자 ID
     * Response: 없음(void)
     */
    @Transactional
    public void createDefaultGroup(Long userId) {
        if (groupRepository.existsByUser_IdAndIsDefaultTrue(userId)) {
            return; // 멱등 — 이미 기본 그룹 보유
        }
        groupRepository.save(FavoriteGroup.createDefault(userRepository.getReferenceById(userId)));
    }

    /** 기능: 그룹 조회 후 요청 사용자 소유가 아니면 FORBIDDEN, 없으면 FAVORITE_GROUP_NOT_FOUND 예외 */
    private FavoriteGroup loadOwned(Long userId, Long groupId) {
        FavoriteGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FAVORITE_GROUP_NOT_FOUND));
        if (!group.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return group;
    }

    /** 기능: Place 엔티티 + 방문 여부를 FavoritePlaceResponse DTO로 변환 */
    private FavoritePlaceResponse toPlaceResponse(Place p, boolean visited) {
        return new FavoritePlaceResponse(p.getId(), p.getName(), p.getCategory(), p.getAddress(),
                p.getRoadAddress(), p.getLatitude(), p.getLongitude(), visited);
    }
}
