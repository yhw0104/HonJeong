package com.honjeong.mate.domain;

import java.time.LocalDateTime;
import com.honjeong.user.domain.User;
import jakarta.persistence.*;

@Entity
@Table(name = "mates")
public class Mate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mate_user_id", nullable = false)
    private User mateUser;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Mate() {
    }

    private Mate(User user, User mateUser, LocalDateTime now) {
        this.user = user;
        this.mateUser = mateUser;
        this.createdAt = now;
    }

    public static Mate create(User user, User mateUser, LocalDateTime now) {
        return new Mate(user, mateUser, now);
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public User getMateUser() { return mateUser; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
