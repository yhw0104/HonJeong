package com.honjeong.notice.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.honjeong.notice.dto.NoticesResponse;
import com.honjeong.notice.repository.NoticeRepository;

/**
 * 1. 기능: 공지사항 조회 비즈니스 로직 — 조회 전용(등록/수정은 운영자가 DB에서 직접 처리)
 * 2. 사용 Controller: NoticeController
 *
 * <p>[기존 주석] 공지 조회 서비스(읽기 전용). 등록/수정은 운영자가 DB 직접 조작한다.
 */
@Service
public class NoticeService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final NoticeRepository noticeRepository;
    private final Clock clock;

    public NoticeService(NoticeRepository noticeRepository, Clock clock) {
        this.noticeRepository = noticeRepository;
        this.clock = clock;
    }

    /**
     * 기능: 게시된 공지 전체를 조회 — KST 현재 시각 기준으로 미래 게시분(예약/초안) 제외, 핀 우선·게시 최신순 정렬
     * Request: 없음
     * Response: NoticesResponse — 노출할 공지 목록
     *
     * <p>[기존 주석] 게시된 공지 전체 — 핀 먼저, 게시 최신순. 미래 게시분(예약/초안)은 제외.
     */
    @Transactional(readOnly = true)
    public NoticesResponse getNotices() {
        LocalDateTime now = LocalDateTime.now(clock.withZone(KST));
        return NoticesResponse.from(
                noticeRepository.findByPublishedAtLessThanEqualOrderByPinnedDescPublishedAtDescIdDesc(now));
    }
}
