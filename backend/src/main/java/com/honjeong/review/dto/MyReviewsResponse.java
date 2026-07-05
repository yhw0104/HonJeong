package com.honjeong.review.dto;

import java.time.LocalDateTime;
import java.util.List;

/** '내가 쓴 리뷰' 응답 — 인증+일반 리뷰 전체, 작성 최신순. */
public record MyReviewsResponse(List<Item> reviews) {

    public record Item(Long reviewId, Long placeId, String placeName, LocalDateTime visitedAt,
            String content, int tasteRating, int soloFriendlyRating,
            List<String> tags, List<String> imageUrls, boolean authenticated, LocalDateTime createdAt) {}
}
