package com.honjeong.notice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.honjeong.global.common.ApiResponse;
import com.honjeong.notice.dto.NoticesResponse;
import com.honjeong.notice.service.NoticeService;

/**
 * 공지사항 조회 컨트롤러.
 *
 * <p>기본 경로: /api/notices
 *
 * <p>공지사항 REST 컨트롤러. 정식 USER 전용(SecurityConfig 기본 규칙).
 */
@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    /**
     * 공지 목록을 조회한다 — 핀 우선·게시 최신순이며 미래 게시분은 제외한다.
     *
     * <p>사용 화면: 공지사항(NoticesScreen) — 더보기(MoreScreen) &gt; 공지사항으로 진입, 목록과 카테고리 칩 필터.
     *
     * @return 공지 목록(핀 우선·게시 최신순, 미래 게시분 제외)
     */
    @GetMapping
    public ApiResponse<NoticesResponse> list() {
        return ApiResponse.success(noticeService.getNotices());
    }
}
