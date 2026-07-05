package com.honjeong.review.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.honjeong.global.common.ApiResponse;
import com.honjeong.global.security.CurrentUserId;
import com.honjeong.review.dto.DiningHistoryResponse;
import com.honjeong.review.dto.MyReviewsResponse;
import com.honjeong.review.service.ReviewService;

@RestController
public class DiningHistoryController {

    private final ReviewService reviewService;

    public DiningHistoryController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/api/users/me/dining-history")
    public ApiResponse<DiningHistoryResponse> diningHistory(@CurrentUserId Long userId) {
        return ApiResponse.success(reviewService.getDiningHistory(userId));
    }

    /** 내가 쓴 리뷰 전체(인증+일반, 작성 최신순) — '내가 쓴 리뷰' 화면용. */
    @GetMapping("/api/users/me/reviews")
    public ApiResponse<MyReviewsResponse> myReviews(@CurrentUserId Long userId) {
        return ApiResponse.success(reviewService.getMyReviews(userId));
    }
}
