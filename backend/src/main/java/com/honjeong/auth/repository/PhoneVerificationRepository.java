package com.honjeong.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.honjeong.auth.domain.PhoneVerification;

/**
 * 1. 기능: 휴대폰 인증번호 발송 기록의 저장·조회 데이터 접근 (대상 테이블: phone_verifications)
 *
 * <p>[기존 주석] PhoneVerification 영속성 접근. 같은 번호로 여러 번 발송될 수 있어,
 * 검증 시점에는 가장 최근 발송 1건만 가져온다.
 */
public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification, Long> {

    /**
     * 기능: 해당 번호로 발송된 가장 최근 인증번호 기록 1건 조회(인증번호 검증 시 기준 데이터)
     * 쿼리: SELECT * FROM phone_verifications WHERE phone = :phone ORDER BY created_at DESC LIMIT 1
     * Request: phone — 휴대폰 번호 / Response: Optional&lt;PhoneVerification&gt; — 최신 발송 기록(없으면 empty)
     *
     * <p>[기존 주석] 해당 번호의 가장 최근 발송 기록 1건을 조회한다(인증번호 확인 시 사용).
     * 메서드명 해석: phone으로 필터 → created_at 내림차순 정렬 → Top(맨 위 1건).
     * 즉 WHERE phone = ? ORDER BY created_at DESC LIMIT 1.
     */
    Optional<PhoneVerification> findTopByPhoneOrderByCreatedAtDesc(String phone);
}
