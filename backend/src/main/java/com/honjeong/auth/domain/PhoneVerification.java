package com.honjeong.auth.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 휴대폰 인증 발송 기록. 회원과 무관한 발송도 있어 phone 기준(FK 없음).
 * created_at만 가지므로 BaseTimeEntity(updated 포함) 대신 @CreatedDate만 둔다.
 * 인증번호는 단기(3분)·rate-limit 대상이라 P1에서는 평문 저장(추후 해시 하드닝 가능).
 */
@Entity
@Table(name = "phone_verifications")
@EntityListeners(AuditingEntityListener.class)
public class PhoneVerification {

    // PK. DB auto-increment(IDENTITY).
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 인증 대상 휴대폰 번호. 회원과 무관하게 번호 단위로 기록(FK 없음). NOT NULL.
    @Column(nullable = false)
    private String phone;

    // 발송한 인증번호. 단기·rate-limit 대상이라 P1에서는 평문 저장. NOT NULL.
    @Column(nullable = false)
    private String code;

    // 인증번호 만료 시각(보통 발송 후 3분). expires_at 컬럼.
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    // 인증 성공 처리 여부. 기본 false, 검증 통과 시 true. NOT NULL.
    @Column(nullable = false)
    private boolean verified = false;

    // 인증번호 입력 시도 횟수(무차별 대입 방어용 카운터). 기본 0. NOT NULL.
    @Column(nullable = false)
    private int attempts = 0;

    // 생성 시각. updated가 필요 없어 BaseTimeEntity 대신 @CreatedDate만 둔다.
    // AuditingEntityListener가 INSERT 시 자동 주입하며, updatable=false로 이후 수정되지 않게 고정.
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** JPA용 기본 생성자(외부 직접 사용 금지). */
    protected PhoneVerification() {
    }

    /** 내부 전용 생성자. verified=false, attempts=0 기본값으로 시작. 외부에서는 {@link #issue} 팩토리 사용. */
    private PhoneVerification(String phone, String code, LocalDateTime expiresAt) {
        this.phone = phone;
        this.code = code;
        this.expiresAt = expiresAt;
    }

    /**
     * 새 휴대폰 인증 발송 기록을 만드는 정적 팩토리.
     *
     * @param phone     인증 대상 번호
     * @param code      발송한 인증번호
     * @param expiresAt 만료 시각
     * @return verified=false 상태의 새 PhoneVerification
     */
    public static PhoneVerification issue(String phone, String code, LocalDateTime expiresAt) {
        return new PhoneVerification(phone, code, expiresAt);
    }

    /**
     * 인증번호가 만료됐는지 판정한다.
     *
     * @param now 기준 현재 시각
     * @return 만료시각이 now보다 과거(이미 지남)면 true
     */
    public boolean isExpired(LocalDateTime now) {
        return expiresAt.isBefore(now);
    }

    /**
     * 사용자가 입력한 인증번호가 발송한 코드와 일치하는지 확인한다(상태 변경 없음).
     *
     * @param submitted 사용자가 입력한 인증번호
     * @return 저장된 code와 정확히 같으면 true
     */
    public boolean matches(String submitted) {
        return code.equals(submitted);
    }

    /** 인증번호 입력 시도 횟수를 1 증가시킨다(검증 실패/시도 시 호출해 rate-limit 판단에 사용). */
    public void incrementAttempts() {
        this.attempts++;
    }

    /** 인증 성공으로 표시한다. verified를 true로 전환. */
    public void markVerified() {
        this.verified = true;
    }

    // --- 이하 게터: 읽기 전용 접근자(상태 변경 없음) ---

    public Long getId() {
        return id;
    }

    public String getPhone() {
        return phone;
    }

    public boolean isVerified() {
        return verified;
    }

    public int getAttempts() {
        return attempts;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
