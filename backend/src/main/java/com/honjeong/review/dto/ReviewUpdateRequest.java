package com.honjeong.review.dto;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 리뷰 수정 요청 데이터. place·checkIn은 변경 불가라 필드에 없다.
 *
 * @param tasteRating 맛 별점(필수, 1~5)
 * @param soloFriendlyRating 혼밥 적합도 별점(필수, 1~5)
 * @param content 리뷰 본문(선택, 최대 1000자)
 * @param tags 혼밥 친화 태그 목록(선택, SoloFriendlyTags 프리셋만 허용 — 전량 교체)
 * @param imageUrls 첨부 사진 URL 목록(선택, 최대 5장 — 전량 교체)
 *
 * <p>[기존 주석] 리뷰 수정 요청. 별점 2종 필수(1~5). content·tags·imageUrls 선택. place·checkIn은 변경 불가(미포함).
 */
public record ReviewUpdateRequest(
        @NotNull @Min(1) @Max(5) Integer tasteRating,
        @NotNull @Min(1) @Max(5) Integer soloFriendlyRating,
        @Size(max = 1000) String content,
        List<String> tags,
        @Size(max = 5) List<@NotBlank String> imageUrls) {
}
