package com.honjeong.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.honjeong.auth.domain.PhoneVerification;

/**
 * PhoneVerification 영속성 접근. 같은 번호로 여러 번 발송될 수 있어,
 * 검증 시점에는 가장 최근 발송 1건만 가져온다.
 */
public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification, Long> {

    /**
     * 해당 번호의 가장 최근 발송 기록 1건을 조회한다(인증번호 확인 시 사용).
     * 메서드명 해석: phone으로 필터 → created_at 내림차순 정렬 → Top(맨 위 1건).
     * 즉 WHERE phone = ? ORDER BY created_at DESC LIMIT 1.
     */
    Optional<PhoneVerification> findTopByPhoneOrderByCreatedAtDesc(String phone);
}
