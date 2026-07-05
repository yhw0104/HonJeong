package com.honjeong.report.domain;

import java.time.LocalDateTime;
import com.honjeong.user.domain.User;
import jakarta.persistence.*;

/** 신고 접수. 대상은 다형(USER/REVIEW)이라 target_id에 FK가 없고, 표시용 닉네임을 스냅샷으로 보관한다. */
@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportTargetType targetType;

    @Column(nullable = false)
    private Long targetId;

    @Column(nullable = false, length = 20)
    private String targetNickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportReason reasonCode;

    @Column(length = 500)
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

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
