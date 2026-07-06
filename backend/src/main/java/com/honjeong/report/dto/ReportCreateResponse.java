package com.honjeong.report.dto;

/**
 * 신고 접수 결과.
 *
 * @param reportId 접수된 신고 ID
 * @param status 처리 상태 (접수 직후 RECEIVED)
 */
public record ReportCreateResponse(Long reportId, String status) {
}
