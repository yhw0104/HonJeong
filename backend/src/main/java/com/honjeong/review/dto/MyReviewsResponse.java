package com.honjeong.review.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * '내가 쓴 리뷰' 목록 응답 데이터.
 *
 * @param reviews 내 리뷰 목록(작성 최신순)
 *
 * <p>[기존 주석] '내가 쓴 리뷰' 응답 — 인증+일반 리뷰 전체, 작성 최신순.
 */
public record MyReviewsResponse(List<Item> reviews) {

    /**
     * 내가 쓴 리뷰 한 건.
     *
     * @param reviewId 리뷰 ID
     * @param placeId 식당 ID
     * @param placeName 식당 이름
     * @param visitedAt 방문 시각
     * @param content 리뷰 본문
     * @param tasteRating 맛 별점(1~5)
     * @param soloFriendlyRating 혼밥 적합도 별점(1~5)
     * @param tags 혼밥 친화 태그 목록
     * @param imageUrls 첨부 사진 URL 목록
     * @param authenticated 인증(체크인 연결) 리뷰 여부
     * @param createdAt 작성 시각
     */
    public record Item(Long reviewId, Long placeId, String placeName, LocalDateTime visitedAt,
            String content, int tasteRating, int soloFriendlyRating,
            List<String> tags, List<String> imageUrls, boolean authenticated, LocalDateTime createdAt) {}
}
