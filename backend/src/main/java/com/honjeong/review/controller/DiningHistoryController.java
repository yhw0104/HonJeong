package com.honjeong.review.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.honjeong.global.common.ApiResponse;
import com.honjeong.global.security.CurrentUserId;
import com.honjeong.review.dto.DiningHistoryResponse;
import com.honjeong.review.dto.MyReviewsResponse;
import com.honjeong.review.service.ReviewService;

/**
 * 내 혼밥 기록(방문 타임라인)과 내가 쓴 리뷰 조회 컨트롤러.
 *
 * <p>기본 경로: /api/users/me
 */
@RestController
public class DiningHistoryController {

    private final ReviewService reviewService;

    public DiningHistoryController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * 내 방문 타임라인과 요약 통계를 조회한다.
     *
     * <p>사용 화면: 내 혼밥 기록(DiningHistoryScreen).
     *
     * @param userId 인증 사용자 ID
     * @return 요약 통계(총 체크인·일기·방문 식당·이달 체크인) + 체크인별 방문 이력과 연결 리뷰
     */
    @GetMapping("/api/users/me/dining-history")
    public ApiResponse<DiningHistoryResponse> diningHistory(@CurrentUserId Long userId) {
        return ApiResponse.success(reviewService.getDiningHistory(userId));
    }

    /**
     * 내가 쓴 리뷰 전체(인증+일반)를 작성 최신순으로 조회한다.
     *
     * <p>사용 화면: 내가 쓴 리뷰(MyReviewsScreen).
     *
     * @param userId 인증 사용자 ID
     * @return 인증+일반 리뷰 전체(작성 최신순), 식당명·별점·태그·사진 포함
     */
    @GetMapping("/api/users/me/reviews")
    public ApiResponse<MyReviewsResponse> myReviews(@CurrentUserId Long userId) {
        return ApiResponse.success(reviewService.getMyReviews(userId));
    }
}
