package com.honjeong.report.dto;

import java.time.LocalDateTime;
import com.honjeong.report.domain.Report;

/** 내 신고 내역 항목. */
public record MyReportResponse(Long id, String targetType, String targetNickname,
        String reasonCode, String detail, String status, LocalDateTime createdAt) {

    public static MyReportResponse from(Report r) {
        return new MyReportResponse(r.getId(), r.getTargetType().name(), r.getTargetNickname(),
                r.getReasonCode().name(), r.getDetail(), r.getStatus().name(), r.getCreatedAt());
    }
}
