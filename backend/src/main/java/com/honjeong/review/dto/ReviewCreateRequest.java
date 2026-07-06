package com.honjeong.review.dto;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 리뷰 작성 요청 데이터.
 *
 * @param placeId 리뷰 대상 식당 ID(필수)
 * @param checkInId 연결할 체크인 ID(선택 — 없으면 해당 식당의 24시간 내 최근 체크인 자동 연결 시도)
 * @param tasteRating 맛 별점(필수, 1~5)
 * @param soloFriendlyRating 혼밥 적합도 별점(필수, 1~5)
 * @param content 리뷰 본문(선택, 최대 1000자)
 * @param tags 혼밥 친화 태그 목록(선택, SoloFriendlyTags 프리셋만 허용)
 * @param imageUrls 첨부 사진 URL 목록(선택, 최대 5장)
 *
 * <p>[기존 주석] 리뷰 작성 요청. 별점 2종 필수(1~5). checkInId·content·tags·imageUrls 선택.
 */
public record ReviewCreateRequest(
        @NotNull Long placeId,
        Long checkInId,
        @NotNull @Min(1) @Max(5) Integer tasteRating,
        @NotNull @Min(1) @Max(5) Integer soloFriendlyRating,
        @Size(max = 1000) String content,
        List<String> tags,
        @Size(max = 5) List<@NotBlank String> imageUrls) {
}
