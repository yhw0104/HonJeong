package com.honjeong.review.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.honjeong.global.common.ApiResponse;
import com.honjeong.global.security.CurrentUserId;
import com.honjeong.review.dto.PlacePhotoResponse;
import com.honjeong.review.dto.PlaceReviewResponse;
import com.honjeong.review.dto.PlaceReviewSummaryResponse;
import com.honjeong.review.dto.ReviewContextResponse;
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
     * 리뷰를 쓰기 전에, 지금 이 식당에 쓰면 혼밥 인증으로 연결될 체크인이 있는지 조회한다.
     *
     * <p>사용 화면: 식당 상세의 '리뷰 쓰기' — 앱이 이 값으로 혼밥 리뷰 화면(혼밥 별점·태그를 묻는다)과
     * 일반 리뷰 화면(묻지 않는다) 중 하나를 연다. 받은 id를 작성 요청에 그대로 되돌려 보내므로
     * 화면에서 물어본 것과 서버가 저장하는 것이 어긋나지 않는다.
     *
     * @param userId 인증 사용자 ID
     * @param placeId 조회할 식당 ID
     * @return 연결될 체크인 ID(없으면 null)
     */
    @GetMapping("/{placeId}/review-context")
    public ApiResponse<ReviewContextResponse> reviewContext(@CurrentUserId Long userId, @PathVariable Long placeId) {
        return ApiResponse.success(reviewService.getReviewContext(userId, placeId));
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
