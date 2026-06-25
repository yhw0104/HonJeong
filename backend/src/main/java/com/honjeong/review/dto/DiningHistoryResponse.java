package com.honjeong.review.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DiningHistoryResponse(Summary summary, List<Entry> entries) {

    public record Summary(long totalCheckIns, long totalReviews, long distinctPlaces, long thisMonthCheckIns) {}

    public record Entry(Long checkInId, Long placeId, String placeName, LocalDateTime visitedAt,
            String status, ReviewBrief review) {}

    public record ReviewBrief(Long reviewId, String content, int tasteRating, int soloFriendlyRating,
            List<String> tags) {}
}
