package com.honjeong.auth.domain;

import java.time.LocalDateTime;

import com.honjeong.global.common.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 서버가 보관하는 리프레시 토큰(원문이 아닌 SHA-256 해시)을 나타내는 엔티티.
 * (매핑 테이블: refresh_tokens)
 *
 * <p>재발급 시 회전(기존 revoke + 신규 발급)하고, 로그아웃/탈취 시 revoke로 무효화한다.
 * user_id는 연관관계 없이 단순 컬럼으로 매핑해 결합도를 낮춘다.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken extends BaseTimeEntity {

    /** PK. DB auto-increment에 위임(IDENTITY). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 토큰 소유 회원 식별자. User와 연관관계(@ManyToOne) 없이 단순 Long 컬럼(user_id)으로 둬 결합도를 낮춘다. */
    @Column(nullable = false)
    private Long userId;

    /** 토큰 원문이 아니라 해시값을 저장(DB 유출 시에도 원문 복원을 막기 위함). NOT NULL. */
    @Column(nullable = false)
    private String tokenHash;

    /** 만료 시각. 이 시각 이후로는 사용 불가. expires_at 컬럼으로 매핑. */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /** 회수(무효화) 여부. 로그아웃·재발급 회전·탈취 대응 시 true가 된다. 기본 false. */
    @Column(nullable = false)
    private boolean revoked = false;

    /** JPA용 기본 생성자(외부 직접 사용 금지). */
    protected RefreshToken() {
    }

    /**
     * 내부 전용 생성자. revoked는 false로 초기화한다.
     * 외부에서는 {@link #issue(Long, String, LocalDateTime)} 팩토리로만 생성한다.
     */
    private RefreshToken(Long userId, String tokenHash, LocalDateTime expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.revoked = false;
    }

    /**
     * 새 refresh 토큰을 발급(생성)하는 정적 팩토리.
     *
     * @param userId    토큰 소유 회원 id
     * @param tokenHash 토큰 원문의 해시값
     * @param expiresAt 만료 시각
     * @return revoked=false로 초기화된 새 RefreshToken
     */
    public static RefreshToken issue(Long userId, String tokenHash, LocalDateTime expiresAt) {
        return new RefreshToken(userId, tokenHash, expiresAt);
    }

    /**
     * 이 토큰을 회수(무효화)한다. revoked를 true로 바꿔 이후 {@link #isUsable(LocalDateTime)}이 false가 되게 한다.
     * 로그아웃, 재발급 시 기존 토큰 회전, 탈취 대응 등에 쓴다.
     */
    public void revoke() {
        this.revoked = true;
    }

    /**
     * 지금 사용 가능한 토큰인지 판정한다. 사용 가능 = 미회수(!revoked) {@code &&} 미만료(만료시각이 now보다 미래).
     *
     * @param now 판정 기준 현재 시각
     * @return 회수되지 않았고 아직 만료 전이면 true
     */
    public boolean isUsable(LocalDateTime now) {
        return !revoked && expiresAt.isAfter(now);
    }

    // --- 이하 게터: 읽기 전용 접근자(상태 변경 없음) ---

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }
}
