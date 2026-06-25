package com.honjeong.review.controller;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.honjeong.global.common.ApiResponse;
import com.honjeong.review.dto.PlaceReviewResponse;
import com.honjeong.review.service.ReviewService;

@RestController
@RequestMapping("/api/places")
public class PlaceReviewController {

    private final ReviewService reviewService;

    public PlaceReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/{placeId}/reviews")
    public ApiResponse<List<PlaceReviewResponse>> reviews(@PathVariable Long placeId) {
        return ApiResponse.success(reviewService.getPlaceReviews(placeId));
    }
}
