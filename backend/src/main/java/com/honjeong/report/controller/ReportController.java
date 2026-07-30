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
 * <p>신고 REST(FR-108). 접수와 내 내역 조회만 제공한다(처리 상태 변경은 관리자 툴 도입 후).
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * 신고를 접수한다(201 Created). 대상은 USER 또는 REVIEW다.
     *
     * <p>사용 화면: 신고하기(ReportForm)의 유저/리뷰 신고 폼 제출.
     *
     * @param userId 인증 사용자 ID(신고자)
     * @param request targetType(USER/REVIEW), targetId(대상 ID), reasonCode(신고 사유),
     *                detail(상세 내용, 최대 500자)
     * @return 접수된 신고 ID와 처리 상태
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReportCreateResponse> create(@CurrentUserId Long userId,
            @Valid @RequestBody ReportCreateRequest request) {
        return ApiResponse.success(reportService.create(userId, request));
    }

    /**
     * 내 신고 내역을 최신순으로 조회한다.
     *
     * <p>사용 화면: 차단·신고 관리(BlockReport)의 내 신고 내역 목록.
     *
     * @param userId 인증 사용자 ID
     * @return 대상 종류·닉네임, 사유, 상태, 접수 시각
     */
    @GetMapping
    public ApiResponse<List<MyReportResponse>> list(@CurrentUserId Long userId) {
        return ApiResponse.success(reportService.getMyReports(userId));
    }
}
