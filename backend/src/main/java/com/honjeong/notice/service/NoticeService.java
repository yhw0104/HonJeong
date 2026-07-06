package com.honjeong.notice.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.honjeong.notice.dto.NoticesResponse;
import com.honjeong.notice.repository.NoticeRepository;

/** 공지 조회 서비스(읽기 전용). 등록/수정은 운영자가 DB 직접 조작한다. */
@Service
public class NoticeService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final NoticeRepository noticeRepository;
    private final Clock clock;

    public NoticeService(NoticeRepository noticeRepository, Clock clock) {
        this.noticeRepository = noticeRepository;
        this.clock = clock;
    }

    /** 게시된 공지 전체 — 핀 먼저, 게시 최신순. 미래 게시분(예약/초안)은 제외. */
    @Transactional(readOnly = true)
    public NoticesResponse getNotices() {
        LocalDateTime now = LocalDateTime.now(clock.withZone(KST));
        return NoticesResponse.from(
                noticeRepository.findByPublishedAtLessThanEqualOrderByPinnedDescPublishedAtDescIdDesc(now));
    }
}
