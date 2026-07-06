package com.honjeong.review.dto;

import com.honjeong.review.domain.Review;

/**
 * 리뷰 작성/수정 결과 응답 데이터.
 *
 * @param reviewId 리뷰 ID
 * @param placeId 리뷰 대상 식당 ID
 * @param checkInId 연결된 체크인 ID(일반 리뷰면 null)
 * @param authenticated 인증(체크인 연결) 리뷰 여부
 */
public record ReviewResponse(Long reviewId, Long placeId, Long checkInId, boolean authenticated) {
    /** 기능: Review 엔티티를 작성/수정 결과 응답 DTO로 변환 */
    public static ReviewResponse from(Review r) {
        return new ReviewResponse(
                r.getId(),
                r.getPlace().getId(),
                r.getCheckIn() == null ? null : r.getCheckIn().getId(),
                r.isAuthenticated());
    }
}
