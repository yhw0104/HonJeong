package com.honjeong.report.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.honjeong.global.common.DisplayNames;
import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.report.domain.Report;
import com.honjeong.report.domain.ReportReason;
import com.honjeong.report.domain.ReportTargetType;
import com.honjeong.report.dto.MyReportResponse;
import com.honjeong.report.dto.ReportCreateRequest;
import com.honjeong.report.dto.ReportCreateResponse;
import com.honjeong.report.repository.ReportRepository;
import com.honjeong.review.domain.Review;
import com.honjeong.review.repository.ReviewRepository;
import com.honjeong.user.domain.User;
import com.honjeong.user.repository.UserRepository;

/**
 * 유저/리뷰 신고를 접수하고 내 신고 내역을 조회한다 — 대상 닉네임을 스냅샷으로 저장해 대상 삭제·탈퇴 후에도 내역이 유지되도록 함.
 *
 * <p>사용처: ReportController.
 * <p>신고 접수 서비스. 대상(USER/REVIEW)을 확인해 표시용 닉네임을 스냅샷으로 저장한다 —
 * 대상 삭제·탈퇴 후에도 신고 내역이 유지되도록 FK 없이 보관한다. 상태는 현재 전부 RECEIVED.
 */
@Service
public class ReportService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final Clock clock;

    public ReportService(ReportRepository reportRepository, UserRepository userRepository,
            ReviewRepository reviewRepository, Clock clock) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
        this.clock = clock;
    }

    /**
     * 신고를 접수한다 — 대상 유효성·자기신고 검증 후 닉네임 스냅샷과 함께 저장(상태 RECEIVED).
     *
     * @param reporterId 신고자 ID
     * @param request ReportCreateRequest(targetType, targetId, reasonCode, detail)
     * @return 접수된 신고 ID, 처리 상태
     */
    @Transactional
    public ReportCreateResponse create(Long reporterId, ReportCreateRequest request) {
        ReportTargetType targetType = parseTargetType(request.targetType());
        ReportReason reason = parseReason(request.reasonCode());
        String targetNickname = resolveTargetNickname(reporterId, targetType, request.targetId());
        Report saved = reportRepository.save(Report.create(
                userRepository.getReferenceById(reporterId), targetType, request.targetId(),
                targetNickname, reason, request.detail(),
                LocalDateTime.ofInstant(clock.instant(), KST)));
        return new ReportCreateResponse(saved.getId(), saved.getStatus().name());
    }

    /**
     * 신고 대상을 확인하고 표시용 닉네임을 반환한다.
     * <p>대상을 확인하고 표시용 닉네임을 돌려준다. 자기 자신/내 리뷰는 REPORT_SELF, 없으면 REPORT_TARGET_NOT_FOUND.
     */
    private String resolveTargetNickname(Long reporterId, ReportTargetType targetType, Long targetId) {
        if (targetType == ReportTargetType.USER) {
            User target = userRepository.findById(targetId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_TARGET_NOT_FOUND));
            if (target.getId().equals(reporterId)) {
                throw new BusinessException(ErrorCode.REPORT_SELF);
            }
            // 탈퇴자는 닉네임이 null이라 그대로 저장하면 target_nickname NOT NULL 위반(500)이 난다 — '알 수 없음'으로 대체.
            return DisplayNames.nicknameOrUnknown(target.getNickname());
        }
        Review review = reviewRepository.findById(targetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_TARGET_NOT_FOUND));
        if (review.getUser().getId().equals(reporterId)) {
            throw new BusinessException(ErrorCode.REPORT_SELF);
        }
        // 탈퇴자는 닉네임이 null이라 그대로 저장하면 target_nickname NOT NULL 위반(500)이 난다 — '알 수 없음'으로 대체.
        return DisplayNames.nicknameOrUnknown(review.getUser().getNickname());
    }

    /**
     * 요청자의 신고 내역을 최신순으로 조회한다.
     *
     * @param userId 신고자(요청 사용자) ID
     * @return 내 신고 내역(최신순)
     */
    @Transactional(readOnly = true)
    public List<MyReportResponse> getMyReports(Long userId) {
        return reportRepository.findAllByReporter_IdOrderByCreatedAtDesc(userId).stream()
                .map(MyReportResponse::from)
                .toList();
    }

    /** 기능: 문자열을 ReportTargetType으로 파싱한다(실패 시 400 INVALID_INPUT) */
    private ReportTargetType parseTargetType(String raw) {
        try {
            return ReportTargetType.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "잘못된 targetType입니다.");
        }
    }

    /** 기능: 문자열을 ReportReason으로 파싱한다(실패 시 400 INVALID_INPUT) */
    private ReportReason parseReason(String raw) {
        try {
            return ReportReason.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "잘못된 reasonCode입니다.");
        }
    }
}
