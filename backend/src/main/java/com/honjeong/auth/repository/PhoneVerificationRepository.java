package com.honjeong.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * 기능: 해당 번호로 발송된 인증 기록을 전부 삭제(탈퇴 시 개인정보 정리용)
     * 쿼리: DELETE FROM phone_verifications WHERE phone = :phone
     * Request: phone — 대상 휴대폰 번호 / Response: int — 삭제된 행 수
     *
     * <p>이 테이블은 {@code users} FK가 없어(번호 단위 기록) 탈퇴 시 FK 기반 정리 스윕에 걸리지 않는다.
     * 원문 휴대폰 번호가 남는 테이블이라 phone 값을 키로 별도 삭제해야 한다.
     */
    // clearAutomatically 금지 — AccountWithdrawalService.deletePersonalData Javadoc 참조
    @Modifying
    @Query("DELETE FROM PhoneVerification pv WHERE pv.phone = :phone")
    int deleteAllByPhone(@Param("phone") String phone);
}
