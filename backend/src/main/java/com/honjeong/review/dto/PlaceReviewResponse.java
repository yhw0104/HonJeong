package com.honjeong.review.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.honjeong.review.domain.Review;
import com.honjeong.review.domain.ReviewTag;

public record PlaceReviewResponse(
        Long reviewId, Author user, LocalDateTime visitedAt, String content,
        int tasteRating, int soloFriendlyRating, List<String> tags, boolean authenticated, boolean mine) {

    public record Author(String nickname) {}

    public static PlaceReviewResponse from(Review r, Long currentUserId) {
        return new PlaceReviewResponse(
                r.getId(),
                new Author(r.getUser().getNickname()),
                r.getVisitedAt(),
                r.getContent(),
                r.getTasteRating(),
                r.getSoloFriendlyRating(),
                r.getTags().stream().map(ReviewTag::getTag).toList(),
                r.isAuthenticated(),
                r.getUser().getId().equals(currentUserId));
    }
}
