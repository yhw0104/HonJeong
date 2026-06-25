package com.honjeong.review.dto;

import java.util.List;

public record PlaceReviewSummaryResponse(
        Long placeId, long reviewCount, Double avgTasteRating, Double avgSoloFriendlyRating,
        List<TagCount> topTags) {

    public record TagCount(String tag, long count) {}
}
