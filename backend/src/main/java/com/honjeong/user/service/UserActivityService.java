package com.honjeong.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.honjeong.checkin.repository.CheckInRepository;
import com.honjeong.favorite.repository.FavoriteRepository;
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

    public UserActivityService(CheckInRepository checkInRepository, ReviewRepository reviewRepository,
            FavoriteRepository favoriteRepository) {
        this.checkInRepository = checkInRepository;
        this.reviewRepository = reviewRepository;
        this.favoriteRepository = favoriteRepository;
    }

    /**
     * 사용자의 활동요약(혼밥·리뷰·즐겨찾기 카운트)을 집계한다. 메이트 수는 도메인 도입 전까지 0이다.
     *
     * @param userId 조회 대상 회원 식별자(JWT sub)
     * @return 카운트 묶음 {@link ActivitySummaryResponse}
     */
    @Transactional(readOnly = true)
    public ActivitySummaryResponse getActivitySummary(long userId) {
        long checkInCount = checkInRepository.countByUser_Id(userId);
        long reviewCount = reviewRepository.countByUser_Id(userId);
        long favoriteCount = favoriteRepository.countDistinctPlaceByUserId(userId);
        long mateCount = 0L; // 메이트 도메인 도입 시 카운트 소스 연결
        return new ActivitySummaryResponse(checkInCount, reviewCount, favoriteCount, mateCount);
    }
}
