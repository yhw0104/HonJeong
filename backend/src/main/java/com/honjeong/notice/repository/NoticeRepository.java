package com.honjeong.notice.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import com.honjeong.notice.domain.Notice;

/**
 * 공지사항 데이터 접근 (대상 테이블: notices).
 */
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    /**
     * 게시 시각이 지난 공지 전체를 핀 우선·게시 최신순으로 조회.
     * <p>게시된 공지 목록 — 핀 먼저, 그 안에서 게시 최신순(동시각은 id 역순).
     *
     * @param now 현재 시각(KST) — 이보다 미래인 예약/초안 공지는 제외
     * @return 노출할 공지 전체
     */
    List<Notice> findByPublishedAtLessThanEqualOrderByPinnedDescPublishedAtDescIdDesc(LocalDateTime now);
}
