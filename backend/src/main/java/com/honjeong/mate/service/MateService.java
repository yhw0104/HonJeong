package com.honjeong.mate.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.honjeong.checkin.domain.CheckIn;
import com.honjeong.checkin.repository.CheckInRepository;
import com.honjeong.checkin.repository.CheckInRepository.CheckInCountRow;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.mate.domain.Mate;
import com.honjeong.mate.dto.MateResponse;
import com.honjeong.mate.repository.MateRepository;
import com.honjeong.meal.repository.MealRequestRepository;
import com.honjeong.meal.repository.MealRequestRepository.MealPairRow;
import com.honjeong.user.domain.User;

@Service
public class MateService {

    private final MateRepository mateRepository;
    private final CheckInRepository checkInRepository;
    private final MealRequestRepository mealRequestRepository;

    public MateService(MateRepository mateRepository, CheckInRepository checkInRepository,
            MealRequestRepository mealRequestRepository) {
        this.mateRepository = mateRepository;
        this.checkInRepository = checkInRepository;
        this.mealRequestRepository = mealRequestRepository;
    }

    @Transactional(readOnly = true)
    public List<MateResponse> getMyMates(Long userId) {
        List<Mate> mates = mateRepository.findMatesWithUserByUserId(userId);
        if (mates.isEmpty()) {
            return List.of();
        }
        List<Long> mateIds = mates.stream().map(m -> m.getMateUser().getId()).toList();
        Map<Long, CheckIn> activeByUser = checkInRepository.findActiveWithPlaceByUserIds(mateIds).stream()
                .collect(Collectors.toMap(c -> c.getUser().getId(), Function.identity(), (a, b) -> a));
        Map<Long, Long> countByUser = checkInRepository.countByUserIds(mateIds).stream()
                .collect(Collectors.toMap(CheckInCountRow::getUserId, CheckInCountRow::getCnt));
        // 함께 먹음(나↔각 메이트 수락 건수)을 한 번의 조회로 집계한다(메이트별 count N+1 방지).
        Map<Long, Long> togetherByUser = new HashMap<>();
        for (MealPairRow row : mealRequestRepository.findAcceptedPairsForUser(userId)) {
            Long otherId = row.getFromId().equals(userId) ? row.getToId() : row.getFromId();
            togetherByUser.merge(otherId, 1L, Long::sum);
        }

        return mates.stream().map(m -> {
            User mate = m.getMateUser();
            CheckIn active = activeByUser.get(mate.getId());
            long checkInCount = countByUser.getOrDefault(mate.getId(), 0L);
            long mealsTogether = togetherByUser.getOrDefault(mate.getId(), 0L);
            return new MateResponse(
                    mate.getId(),
                    mate.getNickname(),
                    mate.getProfileImageUrl(),
                    mate.getDiningStyle() == null ? null : mate.getDiningStyle().name(),
                    mate.getRegion(),
                    active != null,
                    active != null ? active.getPlace().getId() : null,
                    active != null ? active.getPlace().getName() : null,
                    active != null ? active.getStartedAt() : null,
                    checkInCount,
                    mealsTogether,
                    m.getCreatedAt());
        }).toList();
    }

    @Transactional
    public void deleteMate(Long userId, Long mateUserId) {
        Mate forward = mateRepository.findByUser_IdAndMateUser_Id(userId, mateUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATE_NOT_FOUND));
        mateRepository.delete(forward);
        mateRepository.findByUser_IdAndMateUser_Id(mateUserId, userId).ifPresent(mateRepository::delete);
    }
}
