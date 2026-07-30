package com.honjeong.auth.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 온보딩 시 사용자의 약관 동의 내역을 나타내는 엔티티.
 * (매핑 테이블: terms_agreements)
 *
 * <p>사용자당 1행이며 user_id에 UNIQUE 제약이 걸린다. 필수 4종(age·service·privacy·location)에 모두
 * 동의해야 온보딩을 통과하고, marketing은 선택이다({@code AuthService.agreeTerms}가 검증).
 */
@Entity
@Table(name = "terms_agreements")
public class TermsAgreement {

    /** PK. DB auto-increment(IDENTITY). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 동의 주체 회원 id. 사용자당 1행이라 user_id에 UNIQUE 제약(Flyway 스키마)이 걸린다. 연관관계 없이 Long. */
    @Column(nullable = false)
    private Long userId;

    /** 만 14세 이상 확인(필수). NOT NULL. */
    @Column(nullable = false)
    private boolean age;

    /** 서비스 이용약관 동의(필수). NOT NULL. */
    @Column(nullable = false)
    private boolean service;

    /** 개인정보 처리방침 동의(필수). NOT NULL. */
    @Column(nullable = false)
    private boolean privacy;

    /** 위치기반 서비스 동의(필수). NOT NULL. */
    @Column(nullable = false)
    private boolean location;

    /** 마케팅 정보 수신 동의(선택). NOT NULL. */
    @Column(nullable = false)
    private boolean marketing;

    /** 동의 시각. agreed_at 컬럼. */
    @Column(nullable = false)
    private LocalDateTime agreedAt;

    /** JPA용 기본 생성자(외부 직접 사용 금지). */
    protected TermsAgreement() {
    }

    /** 내부 전용 생성자. 외부에서는 {@link #of} 팩토리로만 생성한다. */
    private TermsAgreement(Long userId, boolean age, boolean service, boolean privacy, boolean location,
            boolean marketing, LocalDateTime agreedAt) {
        this.userId = userId;
        this.age = age;
        this.service = service;
        this.privacy = privacy;
        this.location = location;
        this.marketing = marketing;
        this.agreedAt = agreedAt;
    }

    /**
     * 약관 동의 1행을 생성하는 정적 팩토리.
     *
     * @param userId    동의 주체 회원 id
     * @param age       만 14세 이상 확인 여부(필수)
     * @param service   서비스 이용약관 동의 여부(필수)
     * @param privacy   개인정보 처리방침 동의 여부(필수)
     * @param location  위치기반 서비스 동의 여부(필수)
     * @param marketing 마케팅 수신 동의 여부(선택)
     * @param agreedAt  동의 시각
     * @return 새 TermsAgreement 인스턴스
     */
    public static TermsAgreement of(Long userId, boolean age, boolean service, boolean privacy, boolean location,
            boolean marketing, LocalDateTime agreedAt) {
        return new TermsAgreement(userId, age, service, privacy, location, marketing, agreedAt);
    }

    // --- 이하 게터: 읽기 전용 접근자(상태 변경 없음) ---

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public boolean isAge() {
        return age;
    }

    public boolean isService() {
        return service;
    }

    public boolean isPrivacy() {
        return privacy;
    }

    public boolean isLocation() {
        return location;
    }

    public boolean isMarketing() {
        return marketing;
    }
}
