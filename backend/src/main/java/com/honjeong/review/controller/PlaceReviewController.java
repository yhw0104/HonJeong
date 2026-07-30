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
 *
 * <p><b>인가:</b> USER 전용(기본 규칙).
 */
@RestController
@RequestMapping("/api/places")
public class PlaceReviewController {

    private final ReviewService reviewService;

    public PlaceReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * 식당의 리뷰 목록을 조회한다(차단 상대 리뷰 제외).
     *
     * <p>사용 화면: 식당 상세(RestaurantDetailScreen)의 리뷰탭 목록.
     *
     * @param userId 인증 사용자 ID(차단 필터·내 리뷰 판정 기준)
     * @param placeId 조회할 식당 ID
     * @return 작성자 닉네임, 별점 2종, 태그, 사진, 인증 여부, 내 리뷰 여부를 담은 리뷰 목록
     */
    @GetMapping("/{placeId}/reviews")
    public ApiResponse<List<PlaceReviewResponse>> reviews(@CurrentUserId Long userId, @PathVariable Long placeId) {
        return ApiResponse.success(reviewService.getPlaceReviews(placeId, userId));
    }

    /**
     * 식당 리뷰의 평점 평균과 상위 태그를 집계해 조회한다.
     *
     * <p>사용 화면: 식당 상세(RestaurantDetailScreen)의 평점 평균·상위 태그 표시.
     *
     * @param placeId 조회할 식당 ID
     * @return 리뷰 수, 맛·혼밥 적합도 평균, 상위 태그 5개
     */
    @GetMapping("/{placeId}/review-summary")
    public ApiResponse<PlaceReviewSummaryResponse> summary(@PathVariable Long placeId) {
        return ApiResponse.success(reviewService.getPlaceReviewSummary(placeId));
    }

    /**
     * 식당의 리뷰 사진 목록을 리뷰 최신순으로 조회한다.
     *
     * <p>사용 화면: 식당 상세(RestaurantDetailScreen)의 사진탭 그리드.
     *
     * @param placeId 조회할 식당 ID
     * @return 사진 URL + 출처 리뷰 ID 목록(리뷰 최신순)
     */
    @GetMapping("/{placeId}/photos")
    public ApiResponse<List<PlacePhotoResponse>> photos(@PathVariable Long placeId) {
        return ApiResponse.success(reviewService.getPlacePhotos(placeId));
    }
}
