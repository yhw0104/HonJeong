package com.honjeong.review.dto;

/**
 * 식당 사진탭의 사진 한 장 응답 데이터.
 *
 * @param photoUrl 사진 이미지 URL
 * @param reviewId 사진이 첨부된 출처 리뷰 ID
 *
 * <p>[기존 주석] 식당 사진탭 한 장(사진 url + 출처 리뷰 id).
 */
public record PlacePhotoResponse(String photoUrl, Long reviewId) {
}
