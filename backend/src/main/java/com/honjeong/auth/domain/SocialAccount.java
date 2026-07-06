package com.honjeong.auth.domain;

import com.honjeong.global.common.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 회원과 소셜 로그인 계정(공급자·공급자 사용자 id)의 연동 매핑을 나타내는 엔티티.
 * (엔티티: 매핑 테이블 social_accounts)
 *
 * <p>[기존 주석] 소셜 로그인 연동 매핑. 공급자 토큰은 저장하지 않고 (provider, providerUserId)로 회원을 식별한다.
 * UNIQUE(provider, provider_user_id)는 Flyway 스키마에서 강제.
 */
@Entity
@Table(name = "social_accounts")
public class SocialAccount extends BaseTimeEntity {

    /** PK. DB auto-increment(IDENTITY). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 이 소셜 계정이 연결된 회원 id. 연관관계 없이 단순 Long 컬럼(user_id)으로 둠. */
    @Column(nullable = false)
    private Long userId;

    /** 공급자(KAKAO/APPLE). 문자열로 저장(@Enumerated(EnumType.STRING)). NOT NULL. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;

    /**
     * 공급자가 부여한 사용자 식별자(공급자 내 고유 id). provider_user_id 컬럼으로 매핑.
     * (provider, providerUserId) 조합으로 회원을 식별하며, 이 조합에 UNIQUE 제약이 걸린다.
     */
    @Column(nullable = false)
    private String providerUserId;

    /** 공급자가 내려준 이메일(있을 때만). nullable. */
    private String email;

    /** JPA용 기본 생성자(외부 직접 사용 금지). */
    protected SocialAccount() {
    }

    /** 내부 전용 생성자. 외부에서는 {@link #of(Long, Provider, String, String)} 팩토리로만 생성한다. */
    private SocialAccount(Long userId, Provider provider, String providerUserId, String email) {
        this.userId = userId;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.email = email;
    }

    /**
     * 소셜 계정 연동 매핑을 생성하는 정적 팩토리.
     *
     * @param userId         연결할 회원 id
     * @param provider       소셜 공급자
     * @param providerUserId 공급자 내 사용자 식별자
     * @param email          공급자가 준 이메일(nullable)
     * @return 새 SocialAccount 인스턴스
     */
    public static SocialAccount of(Long userId, Provider provider, String providerUserId, String email) {
        return new SocialAccount(userId, provider, providerUserId, email);
    }

    // --- 이하 게터: 읽기 전용 접근자(상태 변경 없음) ---

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Provider getProvider() {
        return provider;
    }

    public String getProviderUserId() {
        return providerUserId;
    }

    public String getEmail() {
        return email;
    }
}
