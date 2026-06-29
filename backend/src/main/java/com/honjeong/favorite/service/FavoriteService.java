package com.honjeong.favorite.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
public class FavoriteService {

    private final FavoriteGroupRepository groupRepository;
    private final FavoriteRepository favoriteRepository;
    private final PlaceService placeService;

    public FavoriteService(FavoriteGroupRepository groupRepository, FavoriteRepository favoriteRepository,
            PlaceService placeService) {
        this.groupRepository = groupRepository;
        this.favoriteRepository = favoriteRepository;
        this.placeService = placeService;
    }

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

    @Transactional
    public void addPlace(Long userId, Long groupId, Long placeId) {
        FavoriteGroup group = loadOwned(userId, groupId);
        if (favoriteRepository.existsByGroup_IdAndPlace_Id(groupId, placeId)) {
            return; // 멱등 — 이미 담김
        }
        Place place = placeService.getById(placeId);
        favoriteRepository.save(Favorite.of(group, place));
    }

    @Transactional
    public void removePlace(Long userId, Long groupId, Long placeId) {
        loadOwned(userId, groupId);
        favoriteRepository.deleteByGroup_IdAndPlace_Id(groupId, placeId);
    }

    private FavoriteGroup loadOwned(Long userId, Long groupId) {
        FavoriteGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FAVORITE_GROUP_NOT_FOUND));
        if (!group.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return group;
    }
}
