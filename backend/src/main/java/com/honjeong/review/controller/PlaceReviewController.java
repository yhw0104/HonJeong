package com.honjeong.review.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.honjeong.global.common.ApiResponse;
import com.honjeong.global.security.CurrentUserId;
import com.honjeong.review.dto.PlacePhotoResponse;
import com.honjeong.review.dto.PlaceReviewResponse;
import com.honjeong.review.dto.PlaceReviewSummaryResponse;
import com.honjeong.review.service.ReviewService;

/**
 * 식당별 리뷰 목록·리뷰 집계·리뷰 사진 조회 컨트롤러.
 *
 * <p>기본 경로: /api/places
 */
@RestController
@RequestMapping("/api/places")
public class PlaceReviewController {

    private final ReviewService reviewService;

    public PlaceReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * 1. API 주소: GET /api/places/{placeId}/reviews
     * 2. 사용 화면: 식당 상세(RestaurantDetailScreen) — 리뷰탭 목록 표시
     * 3. Request: placeId(경로) — 조회할 식당 ID / 인증 사용자(@CurrentUserId)
     * 4. Response: List<PlaceReviewResponse> — 리뷰 목록(작성자 닉네임, 별점 2종, 태그, 사진, 인증 여부, 내 리뷰 여부; 차단 상대 리뷰 제외)
     */
    @GetMapping("/{placeId}/reviews")
    public ApiResponse<List<PlaceReviewResponse>> reviews(@CurrentUserId Long userId, @PathVariable Long placeId) {
        return ApiResponse.success(reviewService.getPlaceReviews(placeId, userId));
    }

    /**
     * 1. API 주소: GET /api/places/{placeId}/review-summary
     * 2. 사용 화면: 식당 상세(RestaurantDetailScreen) — 평점 평균·상위 태그 집계 표시
     * 3. Request: placeId(경로) — 조회할 식당 ID
     * 4. Response: PlaceReviewSummaryResponse — 리뷰 수, 맛·혼밥 적합도 평균, 상위 태그 5개
     */
    @GetMapping("/{placeId}/review-summary")
    public ApiResponse<PlaceReviewSummaryResponse> summary(@PathVariable Long placeId) {
        return ApiResponse.success(reviewService.getPlaceReviewSummary(placeId));
    }

    /**
     * 1. API 주소: GET /api/places/{placeId}/photos
     * 2. 사용 화면: 식당 상세(RestaurantDetailScreen) — 사진탭 그리드 표시
     * 3. Request: placeId(경로) — 조회할 식당 ID
     * 4. Response: List<PlacePhotoResponse> — 리뷰 사진 목록(사진 URL + 출처 리뷰 ID, 리뷰 최신순)
     */
    @GetMapping("/{placeId}/photos")
    public ApiResponse<List<PlacePhotoResponse>> photos(@PathVariable Long placeId) {
        return ApiResponse.success(reviewService.getPlacePhotos(placeId));
    }
}
