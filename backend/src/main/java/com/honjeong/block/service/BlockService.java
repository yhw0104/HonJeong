package com.honjeong.block.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.honjeong.block.domain.Block;
import com.honjeong.block.dto.BlockedUserResponse;
import com.honjeong.block.repository.BlockRepository;
import com.honjeong.checkin.domain.CheckInStatus;
import com.honjeong.checkin.repository.CheckInRepository;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.mate.domain.MateRequestStatus;
import com.honjeong.mate.repository.MateRepository;
import com.honjeong.mate.repository.MateRequestRepository;
import com.honjeong.meal.repository.MealRequestRepository;
import com.honjeong.user.domain.User;
import com.honjeong.user.repository.UserRepository;

/**
 * 유저 차단 도메인 서비스. 차단 생성 시 기존 관계를 한 트랜잭션에서 자동 정리한다 —
 * ① 메이트 양방향 해제 ② PENDING 메이트 신청 종결(보낸 것 CANCELED/받은 것 DECLINED)
 * ③ PENDING 같이먹기 신청 방향 무관 DECLINED ④ 차단 상대와 TOGETHER 매칭 중이면 양쪽 종료.
 * 정리 과정에서 알림은 발행하지 않는다(거절 알림 미발행 관례).
 */
@Service
public class BlockService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final BlockRepository blockRepository;
    private final UserRepository userRepository;
    private final MateRepository mateRepository;
    private final MateRequestRepository mateRequestRepository;
    private final MealRequestRepository mealRequestRepository;
    private final CheckInRepository checkInRepository;
    private final Clock clock;

    public BlockService(BlockRepository blockRepository, UserRepository userRepository,
            MateRepository mateRepository, MateRequestRepository mateRequestRepository,
            MealRequestRepository mealRequestRepository, CheckInRepository checkInRepository, Clock clock) {
        this.blockRepository = blockRepository;
        this.userRepository = userRepository;
        this.mateRepository = mateRepository;
        this.mateRequestRepository = mateRequestRepository;
        this.mealRequestRepository = mealRequestRepository;
        this.checkInRepository = checkInRepository;
        this.clock = clock;
    }

    @Transactional
    public void block(Long blockerId, Long targetUserId) {
        if (blockerId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.BLOCK_SELF);
        }
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        LocalDateTime now = now();
        try {
            blockRepository.saveAndFlush(Block.create(userRepository.getReferenceById(blockerId), target, now));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.BLOCK_ALREADY);
        }
        cleanUpRelations(blockerId, targetUserId, now);
    }

    /** 차단과 함께 기존 관계를 정리한다. 각 단계는 존재할 때만 수행(멱등). */
    private void cleanUpRelations(Long blockerId, Long targetUserId, LocalDateTime now) {
        // ① 메이트 양방향 해제(관계 없으면 no-op — deleteMate와 달리 404를 던지지 않는다)
        mateRepository.findByUser_IdAndMateUser_Id(blockerId, targetUserId).ifPresent(mateRepository::delete);
        mateRepository.findByUser_IdAndMateUser_Id(targetUserId, blockerId).ifPresent(mateRepository::delete);
        // ② PENDING 메이트 신청 — 내가 보낸 건 취소, 받은 건 거절 성격
        mateRequestRepository.resolvePendingBetween(blockerId, targetUserId, MateRequestStatus.CANCELED, now);
        mateRequestRepository.resolvePendingBetween(targetUserId, blockerId, MateRequestStatus.DECLINED, now);
        // ③ PENDING 같이먹기 신청 — MealRequestStatus에 CANCELED가 없어 방향 무관 DECLINED 통일
        mealRequestRepository.declinePendingBetween(blockerId, targetUserId, now);
        // ④ 차단 상대와 같이 먹는 중이면 양쪽 종료("차단했는데 같이 먹는 중" 모순 방지)
        checkInRepository.findByUser_IdAndStatusIn(blockerId, List.of(CheckInStatus.TOGETHER))
                .filter(c -> c.getMealRequestId() != null)
                .ifPresent(mine -> {
                    List<com.honjeong.checkin.domain.CheckIn> pair =
                            checkInRepository.findTogetherByMealRequestId(mine.getMealRequestId());
                    boolean withBlocked = pair.stream()
                            .anyMatch(c -> c.getUser().getId().equals(targetUserId));
                    if (withBlocked) {
                        pair.forEach(c -> c.end(now));
                    }
                });
    }

    @Transactional
    public void unblock(Long blockerId, Long targetUserId) {
        Block block = blockRepository.findByBlocker_IdAndBlocked_Id(blockerId, targetUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BLOCK_NOT_FOUND));
        blockRepository.delete(block);
    }

    @Transactional(readOnly = true)
    public List<BlockedUserResponse> getMyBlocks(Long userId) {
        return blockRepository.findAllWithBlockedByBlocker(userId).stream()
                .map(BlockedUserResponse::from)
                .toList();
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), KST);
    }
}
