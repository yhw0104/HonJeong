package com.honjeong.report.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.honjeong.global.common.ApiResponse;
import com.honjeong.global.security.CurrentUserId;
import com.honjeong.report.dto.MyReportResponse;
import com.honjeong.report.dto.ReportCreateRequest;
import com.honjeong.report.dto.ReportCreateResponse;
import com.honjeong.report.service.ReportService;
import jakarta.validation.Valid;

/**
 * 사용자/리뷰 신고 접수·조회 컨트롤러.
 *
 * <p>기본 경로: /api/reports
 *
 * <p>[기존 주석] 신고 REST(FR-108). 접수와 내 내역 조회만 제공한다(처리 상태 변경은 관리자 툴 도입 후).
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * 1. API 주소: POST /api/reports
     * 2. 사용 화면: 신고하기(ReportForm) — 유저/리뷰 신고 폼 제출
     * 3. Request: 인증 사용자(@CurrentUserId) / ReportCreateRequest(요청바디) — targetType(USER/REVIEW), targetId(대상 ID), reasonCode(신고 사유), detail(상세 내용, 최대 500자)
     * 4. Response: ReportCreateResponse — 접수된 신고 ID, 처리 상태
     *
     * <p>[기존 주석] 신고 접수(대상: USER/REVIEW).
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReportCreateResponse> create(@CurrentUserId Long userId,
            @Valid @RequestBody ReportCreateRequest request) {
        return ApiResponse.success(reportService.create(userId, request));
    }

    /**
     * 1. API 주소: GET /api/reports
     * 2. 사용 화면: 차단·신고 관리(BlockReport) — 내 신고 내역 목록 표시
     * 3. Request: 인증 사용자(@CurrentUserId)
     * 4. Response: List&lt;MyReportResponse&gt; — 내 신고 내역(최신순): 대상 종류·닉네임, 사유, 상태, 접수 시각
     *
     * <p>[기존 주석] 내 신고 내역(최신순).
     */
    @GetMapping
    public ApiResponse<List<MyReportResponse>> list(@CurrentUserId Long userId) {
        return ApiResponse.success(reportService.getMyReports(userId));
    }
}
