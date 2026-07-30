package com.honjeong.mate.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.honjeong.checkin.domain.CheckIn;
import com.honjeong.checkin.repository.CheckInRepository;
import com.honjeong.checkin.repository.CheckInRepository.CheckInCountRow;
import com.honjeong.checkin.repository.CheckInRepository.TogetherPairRow;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.mate.domain.Mate;
import com.honjeong.mate.dto.MateResponse;
import com.honjeong.mate.repository.MateRepository;
import com.honjeong.user.domain.User;

/**
 * 내 메이트 목록 조회(온라인 상태·통계 합성)와 메이트 해제 비즈니스 로직.
 *
 * <p>사용처: MateController.
 */
@Service
public class MateService {

    private final MateRepository mateRepository;
    private final CheckInRepository checkInRepository;

    public MateService(MateRepository mateRepository, CheckInRepository checkInRepository) {
        this.mateRepository = mateRepository;
        this.checkInRepository = checkInRepository;
    }

    /**
     * 내 메이트 목록을 최신순 조회 — 각 메이트의 현재 체크인(온라인)·누적 체크인 수·함께 먹은 횟수를 일괄 조회로 합성(N+1 방지).
     *
     * @param userId 사용자 ID
     * @return 메이트 목록(없으면 빈 목록)
     */
    @Transactional(readOnly = true)
    public List<MateResponse> getMyMates(Long userId) {
        List<Mate> mates = mateRepository.findMatesWithUserByUserId(userId);
        if (mates.isEmpty()) {
            return List.of();
        }
        List<Long> mateIds = mates.stream().map(m -> m.getMateUser().getId()).toList();
        Map<Long, CheckIn> activeByUser = checkInRepository.findSeekingOrActiveWithPlaceByUserIds(mateIds).stream()
                .collect(Collectors.toMap(c -> c.getUser().getId(), Function.identity(), (a, b) -> a));
        Map<Long, Long> countByUser = checkInRepository.countByUserIds(mateIds).stream()
                .collect(Collectors.toMap(CheckInCountRow::getUserId, CheckInCountRow::getCnt));
        // 함께 먹음(나↔각 메이트 실제 매칭 체크인 pairwise)을 한 번의 조회로 집계한다(메이트별 count N+1 방지).
        Map<Long, Long> togetherByUser = checkInRepository.countTogetherPairsForUser(userId).stream()
                .collect(Collectors.toMap(TogetherPairRow::getPartnerId, TogetherPairRow::getCnt));

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

    /**
     * 메이트 해제 — 내 방향 관계 삭제(없으면 MATE_NOT_FOUND) 후 역방향 관계도 있으면 함께 삭제.
     *
     * @param userId 요청 사용자 ID
     * @param mateUserId 해제할 상대 사용자 ID
     */
    @Transactional
    public void deleteMate(Long userId, Long mateUserId) {
        Mate forward = mateRepository.findByUser_IdAndMateUser_Id(userId, mateUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MATE_NOT_FOUND));
        mateRepository.delete(forward);
        mateRepository.findByUser_IdAndMateUser_Id(mateUserId, userId).ifPresent(mateRepository::delete);
    }
}
