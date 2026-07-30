package com.honjeong.report.domain;

import java.time.LocalDateTime;
import com.honjeong.user.domain.User;
import jakarta.persistence.*;

/**
 * 신고 접수 1건을 나타내는 엔티티.
 * (엔티티면: 매핑 테이블 reports)
 *
 * <p>신고 접수. 대상은 다형(USER/REVIEW)이라 target_id에 FK가 없고, 표시용 닉네임을 스냅샷으로 보관한다.
 */
@Entity
@Table(name = "reports")
public class Report {

    /** 신고 PK */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 신고한 사용자 (FK: reporter_id) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    /** 신고 대상 종류 (USER/REVIEW) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportTargetType targetType;

    /** 신고 대상 ID (다형 참조라 DB FK 없음) */
    @Column(nullable = false)
    private Long targetId;

    /** 신고 시점 대상 닉네임 스냅샷 (대상 삭제·탈퇴 후에도 내역 표시용) */
    @Column(nullable = false, length = 20)
    private String targetNickname;

    /** 신고 사유 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportReason reasonCode;

    /** 상세 내용 (선택, 최대 500자) */
    @Column(length = 500)
    private String detail;

    /** 처리 상태 (관리자 툴 도입 전까지 전부 RECEIVED) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

    /** 접수 시각 (KST, 생성 후 변경 불가) */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Report() {
    }

    private Report(User reporter, ReportTargetType targetType, Long targetId, String targetNickname,
            ReportReason reasonCode, String detail, LocalDateTime now) {
        this.reporter = reporter;
        this.targetType = targetType;
        this.targetId = targetId;
        this.targetNickname = targetNickname;
        this.reasonCode = reasonCode;
        this.detail = detail;
        this.status = ReportStatus.RECEIVED;
        this.createdAt = now;
    }

    public static Report create(User reporter, ReportTargetType targetType, Long targetId, String targetNickname,
            ReportReason reasonCode, String detail, LocalDateTime now) {
        return new Report(reporter, targetType, targetId, targetNickname, reasonCode, detail, now);
    }

    public Long getId() { return id; }
    public User getReporter() { return reporter; }
    public ReportTargetType getTargetType() { return targetType; }
    public Long getTargetId() { return targetId; }
    public String getTargetNickname() { return targetNickname; }
    public ReportReason getReasonCode() { return reasonCode; }
    public String getDetail() { return detail; }
    public ReportStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
