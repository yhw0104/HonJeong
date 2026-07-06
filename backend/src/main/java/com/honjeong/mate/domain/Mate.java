package com.honjeong.mate.domain;

import java.time.LocalDateTime;
import com.honjeong.user.domain.User;
import jakarta.persistence.*;

/**
 * 혼밥 메이트(친구) 관계의 한 방향을 나타내는 엔티티 — 메이트 성립 시 양방향으로 2행 저장된다.
 * (매핑 테이블: mates)
 */
@Entity
@Table(name = "mates")
public class Mate {

    /** 메이트 관계 PK */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 관계의 주체 사용자 (FK: user_id) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 주체의 메이트가 된 상대 사용자 (FK: mate_user_id) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mate_user_id", nullable = false)
    private User mateUser;

    /** 메이트가 된(신청 수락) 시각 — KST naive */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Mate() {
    }

    private Mate(User user, User mateUser, LocalDateTime now) {
        this.user = user;
        this.mateUser = mateUser;
        this.createdAt = now;
    }

    /** 기능: 한 방향(user→mateUser) 메이트 관계 생성 — 양방향 저장 시 두 번 호출된다 */
    public static Mate create(User user, User mateUser, LocalDateTime now) {
        return new Mate(user, mateUser, now);
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public User getMateUser() { return mateUser; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
