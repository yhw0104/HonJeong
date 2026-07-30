package com.honjeong.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.honjeong.auth.domain.PhoneVerification;

/**
 * 휴대폰 인증번호 발송 기록의 저장·조회 데이터 접근. (대상 테이블: phone_verifications)
 *
 * <p>같은 번호로 여러 번 발송될 수 있어, 검증 시점에는 가장 최근 발송 1건만 가져온다.
 */
public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification, Long> {

    /**
     * 해당 번호의 가장 최근 발송 기록 1건을 조회한다(인증번호 확인 시 기준 데이터).
     *
     * <p>메서드명 해석: phone으로 필터 → created_at 내림차순 정렬 → Top(맨 위 1건).
     *
     * @param phone 휴대폰 번호
     * @return 최신 발송 기록(없으면 빈 Optional)
     */
    Optional<PhoneVerification> findTopByPhoneOrderByCreatedAtDesc(String phone);

    /**
     * 해당 번호로 발송된 인증 기록을 전부 삭제한다(탈퇴 시 개인정보 정리용).
     *
     * <p>이 테이블은 {@code users} FK가 없어(번호 단위 기록) 탈퇴 시 FK 기반 정리 스윕에 걸리지 않는다.
     * 원문 휴대폰 번호가 남는 테이블이라 phone 값을 키로 별도 삭제해야 한다.
     *
     * @param phone 대상 휴대폰 번호
     * @return 삭제된 행 수
     */
    // clearAutomatically 금지 — AccountWithdrawalService.deletePersonalData Javadoc 참조
    @Modifying
    @Query("DELETE FROM PhoneVerification pv WHERE pv.phone = :phone")
    int deleteAllByPhone(@Param("phone") String phone);
}
