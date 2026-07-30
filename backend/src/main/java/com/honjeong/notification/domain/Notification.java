package com.honjeong.notification.domain;

import java.time.LocalDateTime;
import com.honjeong.user.domain.User;
import jakarta.persistence.*;

/**
 * 인앱 알림 한 건을 나타내는 엔티티.
 * (엔티티면: 매핑 테이블 notifications)
 *
 * <p>인앱 알림 한 건. 받는 사람(user) 기준으로 쌓이고, 문구는 저장하지 않는다 —
 * 앱이 type + actor 닉네임으로 조립한다(문구 변경이 과거 알림에도 적용되도록).
 */
@Entity
@Table(name = "notifications")
public class Notification {

    /** 알림 id (PK). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 받는 사람. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 알림을 일으킨 상대(신청자/수락자). 탈퇴 시 NULL(DB ON DELETE SET NULL). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actor;

    /** 알림 종류. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    /** 읽음 여부. */
    @Column(nullable = false)
    private boolean isRead;

    /** 발생(생성) 시각. */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Notification() {
    }

    private Notification(User user, User actor, NotificationType type, LocalDateTime now) {
        this.user = user;
        this.actor = actor;
        this.type = type;
        this.isRead = false;
        this.createdAt = now;
    }

    public static Notification create(User user, User actor, NotificationType type, LocalDateTime now) {
        return new Notification(user, actor, type, now);
    }

    /** 개별 읽음 처리(멱등 — 이미 읽음이어도 무해). */
    public void markRead() {
        this.isRead = true;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public User getActor() {
        return actor;
    }

    public NotificationType getType() {
        return type;
    }

    public boolean isRead() {
        return isRead;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
