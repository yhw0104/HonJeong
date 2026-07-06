package com.honjeong.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.honjeong.auth.domain.TermsAgreement;

/**
 * 1. 기능: 약관 동의 기록의 저장·조회 데이터 접근 (대상 테이블: terms_agreements)
 *
 * <p>[기존 주석] TermsAgreement 영속성 접근. 사용자당 약관 동의는 1행이므로 user_id 기준 단건 조회/존재 확인을 제공한다.
 */
public interface TermsAgreementRepository extends JpaRepository<TermsAgreement, Long> {

    /**
     * 기능: 회원 id로 약관 동의 행 단건 조회
     * 쿼리: SELECT * FROM terms_agreements WHERE user_id = :userId
     * Request: userId — 회원 ID / Response: Optional&lt;TermsAgreement&gt; — 동의 기록(없으면 empty)
     *
     * <p>[기존 주석] 회원 id로 약관 동의 행을 조회: WHERE user_id = ?. (사용자당 1행이라 단건) 없으면 Optional.empty().
     */
    Optional<TermsAgreement> findByUserId(Long userId);

    /**
     * 기능: 해당 회원의 약관 동의 기록 존재 여부 확인(agreeTerms 멱등 처리용)
     * 쿼리: SELECT COUNT(*) > 0 FROM terms_agreements WHERE user_id = :userId
     * Request: userId — 회원 ID / Response: boolean — 존재 여부
     *
     * <p>[기존 주석] 해당 회원이 이미 약관에 동의했는지 존재 여부 확인: user_id 행 존재 시 true.
     */
    boolean existsByUserId(Long userId);
}
