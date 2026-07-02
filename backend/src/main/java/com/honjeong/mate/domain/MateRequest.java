package com.honjeong.mate.domain;

import java.time.LocalDateTime;
import com.honjeong.user.domain.User;
import jakarta.persistence.*;

@Entity
@Table(name = "mate_requests")
public class MateRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id", nullable = false)
    private User fromUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_user_id", nullable = false)
    private User toUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MateRequestStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime respondedAt;

    protected MateRequest() {
    }

    private MateRequest(User fromUser, User toUser, LocalDateTime now) {
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.status = MateRequestStatus.PENDING;
        this.createdAt = now;
    }

    public static MateRequest create(User fromUser, User toUser, LocalDateTime now) {
        return new MateRequest(fromUser, toUser, now);
    }

    public void accept(LocalDateTime now) {
        this.status = MateRequestStatus.ACCEPTED;
        this.respondedAt = now;
    }

    public void decline(LocalDateTime now) {
        this.status = MateRequestStatus.DECLINED;
        this.respondedAt = now;
    }

    public void cancel(LocalDateTime now) {
        this.status = MateRequestStatus.CANCELED;
        this.respondedAt = now;
    }

    public boolean isPending() {
        return status == MateRequestStatus.PENDING;
    }

    public boolean isReceivedBy(Long userId) {
        return toUser.getId().equals(userId);
    }

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
