package com.honjeong.mate.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.honjeong.block.repository.BlockRepository;
import com.honjeong.checkin.domain.CheckIn;
import com.honjeong.checkin.domain.CheckInStatus;
import com.honjeong.checkin.repository.CheckInRepository;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.mate.domain.MateRequestStatus;
import com.honjeong.mate.dto.PublicProfileResponse;
import com.honjeong.mate.dto.UserSearchResponse;
import com.honjeong.mate.repository.MateRepository;
import com.honjeong.mate.repository.MateRequestRepository;
import com.honjeong.meal.repository.MealRequestRepository;
import com.honjeong.user.domain.User;
import com.honjeong.user.domain.UserStatus;
import com.honjeong.user.repository.UserFoodPreferenceRepository;
import com.honjeong.user.repository.UserRepository;

@Service
public class MateProfileService {

    private final UserRepository userRepository;
    private final MateRepository mateRepository;
    private final MateRequestRepository mateRequestRepository;
    private final CheckInRepository checkInRepository;
    private final UserFoodPreferenceRepository foodRepository;
    private final MealRequestRepository mealRequestRepository;
    private final BlockRepository blockRepository;

    public MateProfileService(UserRepository userRepository, MateRepository mateRepository,
            MateRequestRepository mateRequestRepository, CheckInRepository checkInRepository,
            UserFoodPreferenceRepository foodRepository, MealRequestRepository mealRequestRepository,
            BlockRepository blockRepository) {
        this.userRepository = userRepository;
        this.mateRepository = mateRepository;
        this.mateRequestRepository = mateRequestRepository;
        this.checkInRepository = checkInRepository;
        this.foodRepository = foodRepository;
        this.mealRequestRepository = mealRequestRepository;
        this.blockRepository = blockRepository;
    }

    @Transactional(readOnly = true)
    public List<UserSearchResponse> searchUsers(Long viewerId, String q) {
        if (q == null || q.isBlank()) {
            return List.of();
        }
        Set<Long> blockedIds = new HashSet<>(blockRepository.findCounterpartIds(viewerId));
        return userRepository
                .findTop20ByNicknameContainingIgnoreCaseAndStatus(q.trim(), UserStatus.ACTIVE)
                .stream()
                .filter(u -> !u.getId().equals(viewerId))
                .filter(u -> !blockedIds.contains(u.getId()))   // 차단 상호 은닉(FR-108)
                .map(u -> new UserSearchResponse(
                        u.getId(), u.getNickname(), u.getProfileImageUrl(), u.getRegion(),
                        u.getDiningStyle() == null ? null : u.getDiningStyle().name(),
                        mateRepository.existsByUser_IdAndMateUser_Id(viewerId, u.getId()),
                        requestStatus(viewerId, u.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public PublicProfileResponse getPublicProfile(Long viewerId, Long targetId) {
        User t = userRepository.findById(targetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        // 차단 관계면 존재 자체를 숨긴다(404) — 차단당한 쪽이 눈치 못 채게(스토킹 방지, NFR-03).
        if (blockRepository.existsBlockBetween(viewerId, targetId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        boolean isMate = mateRepository.existsByUser_IdAndMateUser_Id(viewerId, targetId);

        // 온라인 상태는 메이트 여부와 무관하게 항상 공개(같이먹기는 누구나 신청 가능)
        boolean online = false;
        String currentPlaceName = null;
        Long currentPlaceId = null;
        CheckIn active = checkInRepository.findByUser_IdAndStatus(targetId, CheckInStatus.ACTIVE).orElse(null);
        if (active != null) {
            online = true;
            currentPlaceName = active.getPlace().getName();
            currentPlaceId = active.getPlace().getId();
        }
        List<String> foods = foodRepository.findByUserId(targetId)
                .map(fp -> fp.toFoods()).orElse(List.of());

        return new PublicProfileResponse(
                t.getId(), t.getNickname(), t.getProfileImageUrl(), t.getIntroduction(),
                t.getRegion(),
                t.getGender() == null ? null : t.getGender().name(),
                t.getAgeGroup(),
                t.getDiningStyle() == null ? null : t.getDiningStyle().name(),
                foods,
                checkInRepository.countByUser_IdAndStatusNot(targetId, CheckInStatus.CANCELLED),
                mealRequestRepository.countAcceptedBetween(viewerId, targetId),  // 함께 먹음(나↔대상 수락 건수)
                0L,  // badgeCount — 뱃지 도메인 없음
                online, currentPlaceName, currentPlaceId,
                isMate, requestStatus(viewerId, targetId));
    }

    private String requestStatus(Long viewerId, Long targetId) {
        if (mateRequestRepository
                .findByFromUser_IdAndToUser_IdAndStatus(viewerId, targetId, MateRequestStatus.PENDING).isPresent()) {
            return "PENDING_SENT";
        }
        if (mateRequestRepository
                .findByFromUser_IdAndToUser_IdAndStatus(targetId, viewerId, MateRequestStatus.PENDING).isPresent()) {
            return "PENDING_RECEIVED";
        }
        return "NONE";
    }
}
