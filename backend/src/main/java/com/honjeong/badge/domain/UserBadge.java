package com.honjeong.badge.domain;

import java.time.LocalDateTime;
import jakarta.persistence.*;

/** 사용자가 획득한 뱃지 한 건(행-per-뱃지). badgeKey = BadgeCatalog 상수명. */
@Entity
@Table(name = "user_badges")
public class UserBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "badge_key", nullable = false, length = 32)
    private String badgeKey;

    @Column(name = "earned_at", nullable = false, updatable = false)
    private LocalDateTime earnedAt;

    protected UserBadge() {
    }

    private UserBadge(Long userId, String badgeKey, LocalDateTime earnedAt) {
        this.userId = userId;
        this.badgeKey = badgeKey;
        this.earnedAt = earnedAt;
    }

    public static UserBadge of(Long userId, String badgeKey, LocalDateTime earnedAt) {
        return new UserBadge(userId, badgeKey, earnedAt);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getBadgeKey() {
        return badgeKey;
    }

    public LocalDateTime getEarnedAt() {
        return earnedAt;
    }
}
