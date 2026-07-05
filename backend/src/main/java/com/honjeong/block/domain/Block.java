package com.honjeong.block.domain;

import java.time.LocalDateTime;
import com.honjeong.user.domain.User;
import jakarta.persistence.*;

/** 유저 차단. 한 방향(blocker→blocked)만 저장하며, 상호 은닉은 조회 시 양방향 검사로 구현한다. */
@Entity
@Table(name = "blocks")
public class Block {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", nullable = false)
    private User blocked;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Block() {
    }

    private Block(User blocker, User blocked, LocalDateTime now) {
        this.blocker = blocker;
        this.blocked = blocked;
        this.createdAt = now;
    }

    public static Block create(User blocker, User blocked, LocalDateTime now) {
        return new Block(blocker, blocked, now);
    }

    public Long getId() { return id; }
    public User getBlocker() { return blocker; }
    public User getBlocked() { return blocked; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
