package com.honjeong.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.honjeong.auth.domain.TermsAgreement;

/**
 * 약관 동의 기록의 저장·조회 데이터 접근. (대상 테이블: terms_agreements)
 *
 * <p>사용자당 약관 동의는 1행이므로 user_id 기준 단건 조회/존재 확인을 제공한다.
 */
public interface TermsAgreementRepository extends JpaRepository<TermsAgreement, Long> {

    /**
     * 회원 id로 약관 동의 행을 조회한다. 사용자당 1행이라 단건이다.
     *
     * @param userId 회원 ID
     * @return 동의 기록(없으면 빈 Optional)
     */
    Optional<TermsAgreement> findByUserId(Long userId);

    /**
     * 해당 회원이 이미 약관에 동의했는지 확인한다(agreeTerms 멱등 처리용).
     *
     * @param userId 회원 ID
     * @return 동의 기록이 있으면 true
     */
    boolean existsByUserId(Long userId);
}
