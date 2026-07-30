package com.honjeong.favorite.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.honjeong.badge.service.BadgeService;
import com.honjeong.favorite.domain.Favorite;
import com.honjeong.favorite.domain.FavoriteGroup;
import com.honjeong.favorite.dto.FavoriteStatusGroup;
import com.honjeong.favorite.dto.FavoriteStatusResponse;
import com.honjeong.favorite.repository.FavoriteGroupRepository;
import com.honjeong.favorite.repository.FavoriteRepository;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.place.domain.Place;
import com.honjeong.place.service.PlaceService;

/**
 * 개별 즐겨찾기(그룹-장소 매핑)의 상태 조회·담기·빼기 비즈니스 로직.
 *
 * <p>사용처: FavoriteController, FavoriteGroupController.
 */
@Service
public class FavoriteService {

    private final FavoriteGroupRepository groupRepository;
    private final FavoriteRepository favoriteRepository;
    private final PlaceService placeService;
    private final BadgeService badgeService;

    public FavoriteService(FavoriteGroupRepository groupRepository, FavoriteRepository favoriteRepository,
            PlaceService placeService, BadgeService badgeService) {
        this.groupRepository = groupRepository;
        this.favoriteRepository = favoriteRepository;
        this.placeService = placeService;
        this.badgeService = badgeService;
    }

    /**
     * 특정 장소에 대해 사용자의 전체 그룹 목록과 각 그룹의 담김 여부를 조회.
     *
     * @param userId 요청 사용자 ID
     * @param placeId 장소 ID
     * @return 즐겨찾기 여부(saved) + 그룹별 포함 여부 목록
     */
    @Transactional(readOnly = true)
    public FavoriteStatusResponse getStatus(Long userId, Long placeId) {
        List<FavoriteGroup> groups = groupRepository.findByUser_IdOrderByCreatedAtAsc(userId);
        Set<Long> contained = Set.copyOf(favoriteRepository.findGroupIdsContaining(userId, placeId));
        List<FavoriteStatusGroup> mapped = groups.stream()
                .map(g -> new FavoriteStatusGroup(g.getId(), g.getName(), g.getColor(),
                        contained.contains(g.getId())))
                .toList();
        return new FavoriteStatusResponse(!contained.isEmpty(), mapped);
    }

    /**
     * 소유 검증 후 그룹에 장소를 담기(이미 담겨 있으면 아무것도 안 함 — 멱등).
     *
     * @param userId 요청 사용자 ID
     * @param groupId 대상 그룹 ID
     * @param placeId 담을 장소 ID
     */
    @Transactional
    public void addPlace(Long userId, Long groupId, Long placeId) {
        FavoriteGroup group = loadOwned(userId, groupId);
        if (favoriteRepository.existsByGroup_IdAndPlace_Id(groupId, placeId)) {
            return; // 멱등 — 이미 담김
        }
        Place place = placeService.getById(placeId);
        favoriteRepository.save(Favorite.of(group, place));
        badgeService.checkAndAward(userId, true); // 즐겨찾기 뱃지 지급 체크
    }

    /**
     * 소유 검증 후 그룹에서 장소를 빼기.
     *
     * @param userId 요청 사용자 ID
     * @param groupId 대상 그룹 ID
     * @param placeId 뺄 장소 ID
     */
    @Transactional
    public void removePlace(Long userId, Long groupId, Long placeId) {
        loadOwned(userId, groupId);
        favoriteRepository.deleteByGroup_IdAndPlace_Id(groupId, placeId);
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
}
