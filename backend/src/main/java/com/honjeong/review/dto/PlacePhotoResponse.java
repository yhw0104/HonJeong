package com.honjeong.review.dto;

/** 식당 사진탭 한 장(사진 url + 출처 리뷰 id). */
public record PlacePhotoResponse(String photoUrl, Long reviewId) {
}
