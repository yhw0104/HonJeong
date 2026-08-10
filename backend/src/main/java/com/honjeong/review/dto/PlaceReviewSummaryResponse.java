package com.honjeong.review.dto;

import java.util.List;

/**
 * 식당 리뷰 집계(평점 평균·태그 빈도) 응답 데이터.
 *
 * @param placeId 식당 ID
 * @param reviewCount 전체 리뷰 수
 * @param soloRatedCount 혼밥 적합도를 평가한 리뷰 수 — <b>혼밥 인증 리뷰만</b> 이 별점을 갖는다(V28).
 *                       "혼밥러 N명 평가"의 N은 {@code reviewCount}가 아니라 이 값이어야 한다
 * @param avgTasteRating 맛 별점 평균(소수 1자리, 리뷰 없으면 null)
 * @param avgSoloFriendlyRating 혼밥 적합도 별점 평균(소수 1자리). 혼밥 평가가 하나도 없으면 null —
 *                              리뷰는 있는데 전부 인증이 아닌 경우가 여기 해당한다
 * @param topTags 빈도 상위 태그 목록(최대 5개)
 */
public record PlaceReviewSummaryResponse(
        Long placeId, long reviewCount, long soloRatedCount,
        Double avgTasteRating, Double avgSoloFriendlyRating,
        List<TagCount> topTags) {

    /**
     * 태그별 부착 횟수.
     *
     * @param tag 혼밥 친화 태그 문자열
     * @param count 부착 횟수
     */
    public record TagCount(String tag, long count) {}
}
