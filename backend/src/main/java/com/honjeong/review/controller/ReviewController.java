package com.honjeong.review.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.honjeong.global.common.ApiResponse;
import com.honjeong.global.security.CurrentUserId;
import com.honjeong.review.dto.ReviewCreateRequest;
import com.honjeong.review.dto.ReviewResponse;
import com.honjeong.review.dto.ReviewUpdateRequest;
import com.honjeong.review.service.ReviewService;

import jakarta.validation.Valid;

/**
 * 리뷰(혼밥일기) 작성·수정·삭제 컨트롤러.
 *
 * <p>기본 경로: /api/reviews
 */
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * 1. API 주소: POST /api/reviews
     * 2. 사용 화면: 혼밥일기 작성(DiningLogWriteScreen) — 리뷰 작성 제출
     * 3. Request: ReviewCreateRequest(바디) — placeId, checkInId(선택), tasteRating, soloFriendlyRating, content, tags, imageUrls / 인증 사용자(@CurrentUserId)
     * 4. Response: ReviewResponse — 생성된 리뷰 ID, 식당 ID, 연결 체크인 ID, 인증 여부 (201 Created)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReviewResponse> create(@CurrentUserId Long userId,
            @Valid @RequestBody ReviewCreateRequest request) {
        return ApiResponse.success(reviewService.createReview(userId, request));
    }

    /**
     * 1. API 주소: PATCH /api/reviews/{id}
     * 2. 사용 화면: 혼밥일기 작성(DiningLogWriteScreen) — 기존 리뷰 수정 제출
     * 3. Request: id(경로) — 수정할 리뷰 ID / ReviewUpdateRequest(바디) — tasteRating, soloFriendlyRating, content, tags, imageUrls / 인증 사용자(@CurrentUserId)
     * 4. Response: ReviewResponse — 수정된 리뷰 ID, 식당 ID, 연결 체크인 ID, 인증 여부
     */
    @PatchMapping("/{id}")
    public ApiResponse<ReviewResponse> update(@CurrentUserId Long userId, @PathVariable Long id,
            @Valid @RequestBody ReviewUpdateRequest request) {
        return ApiResponse.success(reviewService.updateReview(userId, id, request));
    }

    /**
     * 1. API 주소: DELETE /api/reviews/{id}
     * 2. 사용 화면: 식당 상세(RestaurantDetailScreen)·내 혼밥 기록(DiningHistoryScreen)·내가 쓴 리뷰(MyReviewsScreen) — 내 리뷰 삭제 버튼
     * 3. Request: id(경로) — 삭제할 리뷰 ID / 인증 사용자(@CurrentUserId)
     * 4. Response: 없음(Void)
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@CurrentUserId Long userId, @PathVariable Long id) {
        reviewService.deleteReview(userId, id);
        return ApiResponse.success(null);
    }
}
