package com.honjeong.block.domain;

import java.time.LocalDateTime;
import com.honjeong.user.domain.User;
import jakarta.persistence.*;

/**
 * 유저 간 차단 관계 한 건을 나타내는 데이터 (매핑 테이블: blocks)
 *
 * <p>[기존 주석] 유저 차단. 한 방향(blocker→blocked)만 저장하며, 상호 은닉은 조회 시 양방향 검사로 구현한다.
 */
@Entity
@Table(name = "blocks")
public class Block {

    /** 차단 PK (자동 증가) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 차단을 건 유저 (FK: blocker_id) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker;

    /** 차단당한 유저 (FK: blocked_id) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", nullable = false)
    private User blocked;

    /** 차단 시각 (KST naive, 생성 후 수정 불가) */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Block() {
    }

    private Block(User blocker, User blocked, LocalDateTime now) {
        this.blocker = blocker;
        this.blocked = blocked;
        this.createdAt = now;
    }

    /** 기능: 차단 엔티티 생성 팩토리 (blocker가 blocked를 now 시각에 차단) */
    public static Block create(User blocker, User blocked, LocalDateTime now) {
        return new Block(blocker, blocked, now);
    }

    public Long getId() { return id; }
    public User getBlocker() { return blocker; }
    public User getBlocked() { return blocked; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
