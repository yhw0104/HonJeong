package com.honjeong.review.dto;

import java.util.List;

/**
 * 식당 리뷰 집계(평점 평균·태그 빈도) 응답 데이터.
 *
 * @param placeId 식당 ID
 * @param reviewCount 리뷰 수
 * @param avgTasteRating 맛 별점 평균(소수 1자리, 리뷰 없으면 null)
 * @param avgSoloFriendlyRating 혼밥 적합도 별점 평균(소수 1자리, 리뷰 없으면 null)
 * @param topTags 빈도 상위 태그 목록(최대 5개)
 */
public record PlaceReviewSummaryResponse(
        Long placeId, long reviewCount, Double avgTasteRating, Double avgSoloFriendlyRating,
        List<TagCount> topTags) {

    /**
     * 태그별 부착 횟수.
     *
     * @param tag 혼밥 친화 태그 문자열
     * @param count 부착 횟수
     */
    public record TagCount(String tag, long count) {}
}
