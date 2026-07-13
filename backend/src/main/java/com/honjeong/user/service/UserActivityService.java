package com.honjeong.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.honjeong.checkin.repository.CheckInRepository;
import com.honjeong.favorite.repository.FavoriteRepository;
import com.honjeong.mate.repository.MateRepository;
import com.honjeong.review.repository.ReviewRepository;
import com.honjeong.user.dto.ActivitySummaryResponse;

/**
 * 1. 기능: 프로필 카드용 활동요약(혼밥·리뷰·즐겨찾기·메이트 카운트) 집계 — cross-domain 읽기 전용 조회
 * 2. 사용 Controller: UserController
 *
 * <p>[기존 주석] 프로필 카드용 활동요약 집계 서비스(읽기 전용). 사용자 도메인이 체크인·리뷰·즐겨찾기 도메인의
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
     * 기능: 사용자의 활동요약 카운트(혼밥·같이먹음·인증 리뷰·즐겨찾기 식당·메이트)를 집계한다
     * Request: userId — 조회 대상 회원 ID(JWT sub)
     * Response: ActivitySummaryResponse — checkInCount(혼밥)·reviewCount·favoriteCount·mateCount·togetherCount(같이먹음)
     *
     * <p>[기존 주석] 사용자의 활동요약(혼밥·리뷰·즐겨찾기·메이트 카운트)을 집계한다.
     * checkInCount는 매칭 안 되고 혼자 먹은(solo) 완료 체크인 수이고, togetherCount는 매칭돼 같이 먹은(together) 수다 —
     * 둘의 합은 기존 총합 집계({@code countCompletedByUser})와 정확히 일치한다.
     *
     * @param userId 조회 대상 회원 식별자(JWT sub)
     * @return 카운트 묶음 {@link ActivitySummaryResponse}
     */
    @Transactional(readOnly = true)
    public ActivitySummaryResponse getActivitySummary(long userId) {
        long checkInCount = checkInRepository.countSoloCompletedByUser(userId);
        long togetherCount = checkInRepository.countTogetherByUser(userId);
        long reviewCount = reviewRepository.countSoloAuthenticatedByUser(userId);  // 더보기 '내 혼밥 기록' 디테일 — 혼밥기록 화면(솔로 인증 일기)과 기준 일치
        long favoriteCount = favoriteRepository.countDistinctPlaceByUserId(userId);
        long mateCount = mateRepository.countByUser_Id(userId);
        return new ActivitySummaryResponse(checkInCount, reviewCount, favoriteCount, mateCount, togetherCount);
    }
}
