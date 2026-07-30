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
     * 리뷰(혼밥일기)를 작성한다(201 Created).
     *
     * <p>사용 화면: 혼밥일기 작성(DiningLogWriteScreen)의 제출.
     *
     * @param userId 인증 사용자 ID(작성자)
     * @param request placeId, checkInId(선택), tasteRating, soloFriendlyRating, content, tags, imageUrls
     * @return 생성된 리뷰 ID, 식당 ID, 연결 체크인 ID, 인증 여부
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReviewResponse> create(@CurrentUserId Long userId,
            @Valid @RequestBody ReviewCreateRequest request) {
        return ApiResponse.success(reviewService.createReview(userId, request));
    }

    /**
     * 내 리뷰를 수정한다. 본인만 가능하다.
     *
     * <p>사용 화면: 혼밥일기 작성(DiningLogWriteScreen)의 기존 리뷰 수정 제출.
     *
     * @param userId 인증 사용자 ID
     * @param id 수정할 리뷰 ID
     * @param request tasteRating, soloFriendlyRating, content, tags, imageUrls
     * @return 수정된 리뷰 ID, 식당 ID, 연결 체크인 ID, 인증 여부
     */
    @PatchMapping("/{id}")
    public ApiResponse<ReviewResponse> update(@CurrentUserId Long userId, @PathVariable Long id,
            @Valid @RequestBody ReviewUpdateRequest request) {
        return ApiResponse.success(reviewService.updateReview(userId, id, request));
    }

    /**
     * 내 리뷰를 삭제한다. 본인만 가능하다.
     *
     * <p>사용 화면: 식당 상세(RestaurantDetailScreen)·내 혼밥 기록(DiningHistoryScreen)·
     * 내가 쓴 리뷰(MyReviewsScreen)의 삭제 버튼.
     *
     * @param userId 인증 사용자 ID
     * @param id 삭제할 리뷰 ID
     * @return 본문 데이터 없음 — 성공 여부만 응답 엔벨로프로 전달
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@CurrentUserId Long userId, @PathVariable Long id) {
        reviewService.deleteReview(userId, id);
        return ApiResponse.success(null);
    }
}
