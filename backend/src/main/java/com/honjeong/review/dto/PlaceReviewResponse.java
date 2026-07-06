package com.honjeong.review.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.honjeong.review.domain.Review;
import com.honjeong.review.domain.ReviewTag;

/**
 * 식당 상세 리뷰탭의 리뷰 한 건 응답 데이터.
 *
 * @param reviewId 리뷰 ID
 * @param user 작성자 정보(닉네임)
 * @param visitedAt 방문 시각
 * @param content 리뷰 본문
 * @param tasteRating 맛 별점(1~5)
 * @param soloFriendlyRating 혼밥 적합도 별점(1~5)
 * @param tags 혼밥 친화 태그 목록
 * @param imageUrls 첨부 사진 URL 목록
 * @param authenticated 인증(체크인 연결) 리뷰 여부
 * @param mine 요청 사용자 본인이 쓴 리뷰인지 여부
 */
public record PlaceReviewResponse(
        Long reviewId, Author user, LocalDateTime visitedAt, String content,
        int tasteRating, int soloFriendlyRating, List<String> tags, List<String> imageUrls,
        boolean authenticated, boolean mine) {

    /**
     * 리뷰 작성자 정보.
     *
     * @param nickname 작성자 닉네임
     */
    public record Author(String nickname) {}

    /** 기능: Review 엔티티와 사진 URL 목록을 리뷰탭 응답 DTO로 변환(내 리뷰 여부 계산 포함) */
    public static PlaceReviewResponse from(Review r, Long currentUserId, List<String> imageUrls) {
        return new PlaceReviewResponse(
                r.getId(),
                new Author(r.getUser().getNickname()),
                r.getVisitedAt(),
                r.getContent(),
                r.getTasteRating(),
                r.getSoloFriendlyRating(),
                r.getTags().stream().map(ReviewTag::getTag).toList(),
                imageUrls,
                r.isAuthenticated(),
                r.getUser().getId().equals(currentUserId));
    }
}
