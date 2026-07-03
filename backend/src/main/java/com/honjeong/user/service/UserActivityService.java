package com.honjeong.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.honjeong.checkin.domain.CheckInStatus;
import com.honjeong.checkin.repository.CheckInRepository;
import com.honjeong.favorite.repository.FavoriteRepository;
import com.honjeong.mate.repository.MateRepository;
import com.honjeong.review.repository.ReviewRepository;
import com.honjeong.user.dto.ActivitySummaryResponse;

/**
 * 프로필 카드용 활동요약 집계 서비스(읽기 전용). 사용자 도메인이 체크인·리뷰·즐겨찾기 도메인의
 * 카운트를 모으는 cross-domain 조회라서 UserService와 분리한다.
 */
@Service
public class UserActivityService {

    private final CheckInRepository checkInRepository;
    private final ReviewRepository reviewRepository;
    private final FavoriteRepository favoriteRepository;
    private final MateRepository mateRepository;

    public UserActivityService(CheckInRepository checkInRepository, ReviewRepository reviewRepository,
            FavoriteRepository favoriteRepository, MateRepository mateRepository) {
        this.checkInRepository = checkInRepository;
        this.reviewRepository = reviewRepository;
        this.favoriteRepository = favoriteRepository;
        this.mateRepository = mateRepository;
    }

    /**
     * 사용자의 활동요약(혼밥·리뷰·즐겨찾기·메이트 카운트)을 집계한다.
     *
     * @param userId 조회 대상 회원 식별자(JWT sub)
     * @return 카운트 묶음 {@link ActivitySummaryResponse}
     */
    @Transactional(readOnly = true)
    public ActivitySummaryResponse getActivitySummary(long userId) {
        long checkInCount = checkInRepository.countByUser_IdAndStatusNot(userId, CheckInStatus.CANCELLED);
        long reviewCount = reviewRepository.countByUser_Id(userId);
        long favoriteCount = favoriteRepository.countDistinctPlaceByUserId(userId);
        long mateCount = mateRepository.countByUser_Id(userId);
        return new ActivitySummaryResponse(checkInCount, reviewCount, favoriteCount, mateCount);
    }
}
