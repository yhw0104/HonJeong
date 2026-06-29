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

    @Transactional(readOnly = true)
    public List<FavoriteGroupSummaryResponse> getGroups(Long userId) {
        return groupRepository.findSummaries(userId);
    }

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

    @Transactional
    public FavoriteGroupSummaryResponse createGroup(Long userId, CreateGroupRequest req) {
        User userRef = userRepository.getReferenceById(userId);
        String color = (req.color() == null || req.color().isBlank()) ? DEFAULT_COLOR : req.color();
        FavoriteGroup group = groupRepository.save(
                FavoriteGroup.create(userRef, req.name(), req.note(), color, false));
        return new FavoriteGroupSummaryResponse(group.getId(), group.getName(), group.getNote(),
                group.getColor(), group.isDefault(), 0L);
    }

    @Transactional
    public FavoriteGroupSummaryResponse updateGroup(Long userId, Long groupId, UpdateGroupRequest req) {
        FavoriteGroup group = loadOwned(userId, groupId);
        group.updateInfo(req.name(), req.note(), req.color());
        long count = favoriteRepository.countByGroup_Id(groupId);
        return new FavoriteGroupSummaryResponse(group.getId(), group.getName(), group.getNote(),
                group.getColor(), group.isDefault(), count);
    }

    @Transactional
    public void deleteGroup(Long userId, Long groupId) {
        FavoriteGroup group = loadOwned(userId, groupId);
        if (group.isDefault()) {
            throw new BusinessException(ErrorCode.DEFAULT_GROUP_NOT_DELETABLE);
        }
        favoriteRepository.deleteByGroup_Id(groupId);
        groupRepository.delete(group);
    }

    @Transactional
    public void createDefaultGroup(Long userId) {
        if (groupRepository.existsByUser_IdAndIsDefaultTrue(userId)) {
            return; // 멱등 — 이미 기본 그룹 보유
        }
        groupRepository.save(FavoriteGroup.createDefault(userRepository.getReferenceById(userId)));
    }

    private FavoriteGroup loadOwned(Long userId, Long groupId) {
        FavoriteGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FAVORITE_GROUP_NOT_FOUND));
        if (!group.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return group;
    }

    private FavoritePlaceResponse toPlaceResponse(Place p, boolean visited) {
        return new FavoritePlaceResponse(p.getId(), p.getName(), p.getCategory(), p.getAddress(),
                p.getRoadAddress(), p.getLatitude(), p.getLongitude(), visited);
    }
}
