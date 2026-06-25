package com.honjeong.review.dto;

import com.honjeong.review.domain.Review;

public record ReviewResponse(Long reviewId, Long placeId, Long checkInId, boolean authenticated) {
    public static ReviewResponse from(Review r) {
        return new ReviewResponse(
                r.getId(),
                r.getPlace().getId(),
                r.getCheckIn() == null ? null : r.getCheckIn().getId(),
                r.isAuthenticated());
    }
}
