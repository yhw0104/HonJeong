package com.honjeong.review.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 내 혼밥 기록(방문 타임라인) 응답 데이터.
 *
 * @param summary 요약 통계(총 체크인·일기·방문 식당·이달 체크인)
 * @param entries 체크인별 방문 이력 목록(최신순)
 */
public record DiningHistoryResponse(Summary summary, List<Entry> entries) {

    /**
     * 방문 기록 요약 통계.
     *
     * @param totalCheckIns 취소 제외 총 체크인 수
     * @param totalReviews 인증 리뷰(일기) 수
     * @param distinctPlaces 방문한 식당 수(중복 제외)
     * @param thisMonthCheckIns 이달(KST 기준) 체크인 수
     */
    public record Summary(long totalCheckIns, long totalReviews, long distinctPlaces, long thisMonthCheckIns) {}

    /**
     * 방문 이력 한 건(체크인 + 연결 리뷰).
     *
     * @param checkInId 체크인 ID
     * @param placeId 방문 식당 ID
     * @param placeName 방문 식당 이름
     * @param visitedAt 방문(체크인 시작) 시각
     * @param status 체크인 상태명(CheckInStatus)
     * @param review 이 방문에 작성한 리뷰 요약(없으면 null)
     */
    public record Entry(Long checkInId, Long placeId, String placeName, LocalDateTime visitedAt,
            String status, ReviewBrief review) {}

    /**
     * 방문에 연결된 리뷰 요약.
     *
     * @param reviewId 리뷰 ID
     * @param content 리뷰 본문
     * @param tasteRating 맛 별점(1~5)
     * @param soloFriendlyRating 혼밥 적합도 별점(1~5)
     * @param tags 혼밥 친화 태그 목록
     * @param imageUrls 첨부 사진 URL 목록
     */
    public record ReviewBrief(Long reviewId, String content, int tasteRating, int soloFriendlyRating,
            List<String> tags, List<String> imageUrls) {}
}
