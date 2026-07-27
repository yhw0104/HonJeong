package com.honjeong.report.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.honjeong.global.exception.BusinessException;
import com.honjeong.global.exception.ErrorCode;
import com.honjeong.report.domain.Report;
import com.honjeong.report.domain.ReportReason;
import com.honjeong.report.domain.ReportStatus;
import com.honjeong.report.domain.ReportTargetType;
import com.honjeong.report.dto.MyReportResponse;
import com.honjeong.report.dto.ReportCreateRequest;
import com.honjeong.report.dto.ReportCreateResponse;
import com.honjeong.report.repository.ReportRepository;
import com.honjeong.review.domain.Review;
import com.honjeong.review.repository.ReviewRepository;
import com.honjeong.user.domain.User;
import com.honjeong.user.repository.UserRepository;

class ReportServiceTest {

    private final ReportRepository reportRepository = mock(ReportRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ReviewRepository reviewRepository = mock(ReviewRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-05T03:00:00Z"), ZoneId.of("Asia/Seoul"));

    private final ReportService service = new ReportService(reportRepository, userRepository,
            reviewRepository, clock);

    private User user(long id) {
        User u = mock(User.class);
        when(u.getId()).thenReturn(id);
        return u;
    }

    private Review review(long id, long authorId, String authorNickname) {
        Review r = mock(Review.class);
        User author = user(authorId);
        when(author.getNickname()).thenReturn(authorNickname);
        when(r.getUser()).thenReturn(author);
        return r;
    }

    @Test
    @DisplayName("USER 신고: 대상 닉네임 스냅샷으로 저장, RECEIVED 반환")
    void create_userTarget() {
        User target = mock(User.class);
        when(target.getId()).thenReturn(2L);
        when(target.getNickname()).thenReturn("상대닉");
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        Report saved = mock(Report.class);
        when(saved.getId()).thenReturn(10L);
        when(saved.getStatus()).thenReturn(ReportStatus.RECEIVED);
        when(reportRepository.save(any(Report.class))).thenReturn(saved);

        ReportCreateResponse res = service.create(1L, new ReportCreateRequest("USER", 2L, "SPAM", null));

        assertThat(res.reportId()).isEqualTo(10L);
        assertThat(res.status()).isEqualTo("RECEIVED");
        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(captor.capture());
        assertThat(captor.getValue().getTargetNickname()).isEqualTo("상대닉");
        assertThat(captor.getValue().getReasonCode()).isEqualTo(ReportReason.SPAM);
    }

    @Test
    @DisplayName("REVIEW 신고: 리뷰 작성자 닉네임 스냅샷")
    void create_reviewTarget() {
        Review review = review(5L, 2L, "글쓴이");
        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review));
        Report saved = mock(Report.class);
        when(saved.getId()).thenReturn(11L);
        when(saved.getStatus()).thenReturn(ReportStatus.RECEIVED);
        when(reportRepository.save(any(Report.class))).thenReturn(saved);

        ReportCreateResponse res = service.create(1L, new ReportCreateRequest("REVIEW", 5L, "ABUSE", "상세 내용"));

        assertThat(res.reportId()).isEqualTo(11L);
        assertThat(res.status()).isEqualTo("RECEIVED");
        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(captor.capture());
        assertThat(captor.getValue().getTargetNickname()).isEqualTo("글쓴이");
        assertThat(captor.getValue().getReasonCode()).isEqualTo(ReportReason.ABUSE);
    }

    @Test
    @DisplayName("탈퇴한 사용자(USER) 신고: 닉네임이 null이어도 '알 수 없음'으로 스냅샷 저장(NOT NULL 위반으로 인한 500 방지)")
    void create_userTarget_withdrawn() {
        User target = mock(User.class);
        when(target.getId()).thenReturn(2L);
        when(target.getNickname()).thenReturn(null);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        Report saved = mock(Report.class);
        when(saved.getId()).thenReturn(12L);
        when(saved.getStatus()).thenReturn(ReportStatus.RECEIVED);
        when(reportRepository.save(any(Report.class))).thenReturn(saved);

        ReportCreateResponse res = service.create(1L, new ReportCreateRequest("USER", 2L, "SPAM", null));

        assertThat(res.reportId()).isEqualTo(12L);
        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(captor.capture());
        assertThat(captor.getValue().getTargetNickname()).isEqualTo("알 수 없음");
    }

    @Test
    @DisplayName("탈퇴한 사용자가 쓴 리뷰(REVIEW) 신고: 작성자 닉네임이 null이어도 '알 수 없음'으로 스냅샷 저장(NOT NULL 위반으로 인한 500 방지)")
    void create_reviewTarget_withdrawnAuthor() {
        Review review = review(5L, 2L, null);
        when(reviewRepository.findById(5L)).thenReturn(Optional.of(review));
        Report saved = mock(Report.class);
        when(saved.getId()).thenReturn(13L);
        when(saved.getStatus()).thenReturn(ReportStatus.RECEIVED);
        when(reportRepository.save(any(Report.class))).thenReturn(saved);

        ReportCreateResponse res = service.create(1L, new ReportCreateRequest("REVIEW", 5L, "ABUSE", null));

        assertThat(res.reportId()).isEqualTo(13L);
        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(captor.capture());
        assertThat(captor.getValue().getTargetNickname()).isEqualTo("알 수 없음");
    }

    @Test
    @DisplayName("자기 자신 USER 신고 → REPORT_SELF")
    void create_selfUser_throws() {
        User self = user(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(self));

        assertThatThrownBy(() -> service.create(1L, new ReportCreateRequest("USER", 1L, "SPAM", null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.REPORT_SELF);
    }

    @Test
    @DisplayName("내가 쓴 리뷰 신고 → REPORT_SELF")
    void create_ownReview_throws() {
        Review myReview = review(5L, 1L, "내닉네임");
        when(reviewRepository.findById(5L)).thenReturn(Optional.of(myReview));

        assertThatThrownBy(() -> service.create(1L, new ReportCreateRequest("REVIEW", 5L, "SPAM", null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.REPORT_SELF);
    }

    @Test
    @DisplayName("대상 없음(USER/REVIEW) → REPORT_TARGET_NOT_FOUND")
    void create_targetMissing_throws() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(1L, new ReportCreateRequest("USER", 2L, "SPAM", null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.REPORT_TARGET_NOT_FOUND);

        when(reviewRepository.findById(5L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(1L, new ReportCreateRequest("REVIEW", 5L, "SPAM", null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.REPORT_TARGET_NOT_FOUND);
    }

    @Test
    @DisplayName("잘못된 targetType/reasonCode 문자열 → INVALID_INPUT")
    void create_badEnums_throw() {
        assertThatThrownBy(() -> service.create(1L, new ReportCreateRequest("POST", 2L, "SPAM", null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);

        assertThatThrownBy(() -> service.create(1L, new ReportCreateRequest("USER", 2L, "WHATEVER", null)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("내 신고 내역 조회 매핑")
    void getMyReports() {
        Report r = mock(Report.class);
        when(r.getId()).thenReturn(1L);
        when(r.getTargetType()).thenReturn(ReportTargetType.USER);
        when(r.getTargetNickname()).thenReturn("상대닉");
        when(r.getReasonCode()).thenReturn(ReportReason.SPAM);
        when(r.getDetail()).thenReturn("상세 내용");
        when(r.getStatus()).thenReturn(ReportStatus.RECEIVED);
        LocalDateTime now = LocalDateTime.now(clock);
        when(r.getCreatedAt()).thenReturn(now);
        when(reportRepository.findAllByReporter_IdOrderByCreatedAtDesc(1L)).thenReturn(List.of(r));

        List<MyReportResponse> res = service.getMyReports(1L);

        assertThat(res).hasSize(1);
        MyReportResponse dto = res.get(0);
        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.targetType()).isEqualTo("USER");
        assertThat(dto.targetNickname()).isEqualTo("상대닉");
        assertThat(dto.reasonCode()).isEqualTo("SPAM");
        assertThat(dto.detail()).isEqualTo("상세 내용");
        assertThat(dto.status()).isEqualTo("RECEIVED");
        assertThat(dto.createdAt()).isEqualTo(now);
    }
}
