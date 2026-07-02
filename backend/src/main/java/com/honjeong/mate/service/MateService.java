package com.honjeong.mate.service;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.honjeong.checkin.domain.CheckIn;
import com.honjeong.checkin.repository.CheckInRepository;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.mate.domain.Mate;
import com.honjeong.mate.dto.MateResponse;
import com.honjeong.mate.repository.MateRepository;
import com.honjeong.user.domain.User;

@Service
public class MateService {

    private final MateRepository mateRepository;
    private final CheckInRepository checkInRepository;
    private final Clock clock;

    public MateService(MateRepository mateRepository, CheckInRepository checkInRepository, Clock clock) {
        this.mateRepository = mateRepository;
        this.checkInRepository = checkInRepository;
        this.clock = clock;
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

        return mates.stream().map(m -> {
            User mate = m.getMateUser();
            CheckIn active = activeByUser.get(mate.getId());
            long checkInCount = checkInRepository.countByUser_Id(mate.getId());
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
                    0L, // mealsTogether — MATE-011 정밀집계는 범위 밖
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
