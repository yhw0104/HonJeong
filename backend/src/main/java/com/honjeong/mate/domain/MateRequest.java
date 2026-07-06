package com.honjeong.mate.domain;

import java.time.LocalDateTime;
import com.honjeong.user.domain.User;
import jakarta.persistence.*;

/**
 * 메이트 신청 한 건을 나타내는 엔티티 — 발신자가 수신자에게 보낸 신청과 그 응답 상태를 기록한다.
 * (매핑 테이블: mate_requests)
 */
@Entity
@Table(name = "mate_requests")
public class MateRequest {

    /** 메이트 신청 PK */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 신청을 보낸 사용자 (FK: from_user_id) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id", nullable = false)
    private User fromUser;

    /** 신청을 받은 사용자 (FK: to_user_id) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_user_id", nullable = false)
    private User toUser;

    /** 신청 상태 (PENDING/ACCEPTED/DECLINED/CANCELED) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MateRequestStatus status;

    /** 신청 생성 시각 — KST naive */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 수락·거절·취소로 종결된 시각 (PENDING이면 null) */
    private LocalDateTime respondedAt;

    protected MateRequest() {
    }

    private MateRequest(User fromUser, User toUser, LocalDateTime now) {
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.status = MateRequestStatus.PENDING;
        this.createdAt = now;
    }

    /** 기능: PENDING 상태의 신규 메이트 신청 생성 */
    public static MateRequest create(User fromUser, User toUser, LocalDateTime now) {
        return new MateRequest(fromUser, toUser, now);
    }

    /** 기능: 신청을 ACCEPTED로 전이하고 응답 시각 기록 */
    public void accept(LocalDateTime now) {
        this.status = MateRequestStatus.ACCEPTED;
        this.respondedAt = now;
    }

    /** 기능: 신청을 DECLINED로 전이하고 응답 시각 기록 */
    public void decline(LocalDateTime now) {
        this.status = MateRequestStatus.DECLINED;
        this.respondedAt = now;
    }

    /** 기능: 신청을 CANCELED로 전이하고 응답 시각 기록 */
    public void cancel(LocalDateTime now) {
        this.status = MateRequestStatus.CANCELED;
        this.respondedAt = now;
    }

    /** 기능: 아직 응답되지 않은(PENDING) 신청인지 여부 */
    public boolean isPending() {
        return status == MateRequestStatus.PENDING;
    }

    /** 기능: 해당 사용자가 이 신청의 수신자인지 여부 */
    public boolean isReceivedBy(Long userId) {
        return toUser.getId().equals(userId);
    }

    /** 기능: 해당 사용자가 이 신청의 발신자인지 여부 */
    public boolean isSentBy(Long userId) {
        return fromUser.getId().equals(userId);
    }

    public Long getId() { return id; }
    public User getFromUser() { return fromUser; }
    public User getToUser() { return toUser; }
    public MateRequestStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getRespondedAt() { return respondedAt; }
}
