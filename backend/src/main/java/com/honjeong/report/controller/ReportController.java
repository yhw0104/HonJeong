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

/** 신고 REST(FR-108). 접수와 내 내역 조회만 제공한다(처리 상태 변경은 관리자 툴 도입 후). */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /** 신고 접수(대상: USER/REVIEW). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReportCreateResponse> create(@CurrentUserId Long userId,
            @Valid @RequestBody ReportCreateRequest request) {
        return ApiResponse.success(reportService.create(userId, request));
    }

    /** 내 신고 내역(최신순). */
    @GetMapping
    public ApiResponse<List<MyReportResponse>> list(@CurrentUserId Long userId) {
        return ApiResponse.success(reportService.getMyReports(userId));
    }
}
