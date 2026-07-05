package com.honjeong.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 신고 접수 요청. enum 문자열은 서비스에서 파싱한다(잘못되면 400 INVALID_INPUT). */
public record ReportCreateRequest(
        @NotBlank String targetType,
        @NotNull Long targetId,
        @NotBlank String reasonCode,
        @Size(max = 500) String detail) {
}
