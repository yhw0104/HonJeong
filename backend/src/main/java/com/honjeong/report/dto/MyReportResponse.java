package com.honjeong.report.dto;

import java.time.LocalDateTime;
import com.honjeong.report.domain.Report;

/**
 * 내 신고 내역 항목.
 *
 * @param id 신고 ID
 * @param targetType 신고 대상 종류 (USER/REVIEW)
 * @param targetNickname 신고 시점 대상 닉네임 스냅샷
 * @param reasonCode 신고 사유 코드
 * @param detail 상세 내용 (없을 수 있음)
 * @param status 처리 상태
 * @param createdAt 접수 시각
 */
public record MyReportResponse(Long id, String targetType, String targetNickname,
        String reasonCode, String detail, String status, LocalDateTime createdAt) {

    public static MyReportResponse from(Report r) {
        return new MyReportResponse(r.getId(), r.getTargetType().name(), r.getTargetNickname(),
                r.getReasonCode().name(), r.getDetail(), r.getStatus().name(), r.getCreatedAt());
    }
}
