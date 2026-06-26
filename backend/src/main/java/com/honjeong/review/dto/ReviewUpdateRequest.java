package com.honjeong.review.dto;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 리뷰 수정 요청. 별점 2종 필수(1~5). content·tags 선택. place·checkIn은 변경 불가(미포함). */
public record ReviewUpdateRequest(
        @NotNull @Min(1) @Max(5) Integer tasteRating,
        @NotNull @Min(1) @Max(5) Integer soloFriendlyRating,
        @Size(max = 1000) String content,
        List<String> tags) {
}
