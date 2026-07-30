package com.honjeong.report.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.honjeong.report.domain.Report;

/**
 * 신고 데이터 접근 (대상 테이블: reports).
 */
public interface ReportRepository extends JpaRepository<Report, Long> {

    /**
     * 특정 신고자의 신고 내역을 접수 시각 내림차순으로 조회.
     *
     * @param reporterId 신고자 ID
     * @return 신고 목록(최신순)
     */
    List<Report> findAllByReporter_IdOrderByCreatedAtDesc(Long reporterId);
}
