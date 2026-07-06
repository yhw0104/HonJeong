package com.honjeong.notice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.honjeong.global.common.ApiResponse;
import com.honjeong.notice.dto.NoticesResponse;
import com.honjeong.notice.service.NoticeService;

/** 공지사항 REST 컨트롤러. 정식 USER 전용(SecurityConfig 기본 규칙). */
@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    /** 공지 목록 — 핀 우선·게시 최신순, 미래 게시분 제외. */
    @GetMapping
    public ApiResponse<NoticesResponse> list() {
        return ApiResponse.success(noticeService.getNotices());
    }
}
