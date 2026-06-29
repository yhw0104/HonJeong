package com.honjeong.review.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.honjeong.global.common.ApiResponse;
import com.honjeong.global.security.CurrentUserId;
import com.honjeong.review.dto.PlacePhotoResponse;
import com.honjeong.review.dto.PlaceReviewResponse;
import com.honjeong.review.dto.PlaceReviewSummaryResponse;
import com.honjeong.review.service.ReviewService;

@RestController
@RequestMapping("/api/places")
public class PlaceReviewController {

    private final ReviewService reviewService;

    public PlaceReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/{placeId}/reviews")
    public ApiResponse<List<PlaceReviewResponse>> reviews(@CurrentUserId Long userId, @PathVariable Long placeId) {
        return ApiResponse.success(reviewService.getPlaceReviews(placeId, userId));
    }

    @GetMapping("/{placeId}/review-summary")
    public ApiResponse<PlaceReviewSummaryResponse> summary(@PathVariable Long placeId) {
        return ApiResponse.success(reviewService.getPlaceReviewSummary(placeId));
    }

    @GetMapping("/{placeId}/photos")
    public ApiResponse<List<PlacePhotoResponse>> photos(@PathVariable Long placeId) {
        return ApiResponse.success(reviewService.getPlacePhotos(placeId));
    }
}
