package com.honjeong.report.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.honjeong.report.domain.Report;

public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findAllByReporter_IdOrderByCreatedAtDesc(Long reporterId);
}
