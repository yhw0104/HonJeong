package com.honjeong.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.honjeong.auth.domain.Provider;
import com.honjeong.auth.domain.SocialAccount;

/**
 * 소셜 계정 연동 매핑의 저장·조회 데이터 접근. (대상 테이블: social_accounts)
 *
 * <p>소셜 로그인 콜백에서 (공급자, 공급자 사용자 id)로 기존 연동 회원을 찾는다.
 */
public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    /**
     * 공급자 + 공급자 사용자 id 조합으로 연동 계정을 조회한다(소셜 로그인 신규/재방문 분기).
     *
     * <p>이 조합에 UNIQUE 제약이 있어 결과는 단건이다. 결과가 있으면 기존 회원 로그인, 없으면 신규 가입
     * 분기로 처리한다.
     *
     * @param provider 소셜 공급자(KAKAO/APPLE)
     * @param providerUserId 공급자 내 사용자 식별자
     * @return 연동 계정(없으면 빈 Optional)
     */
    Optional<SocialAccount> findByProviderAndProviderUserId(Provider provider, String providerUserId);

    /**
     * 사용자의 소셜 계정 연동을 전부 삭제한다(탈퇴 시 재가입 경로 확보용).
     *
     * @param userId 대상 사용자 ID
     * @return 삭제된 행 수
     */
    // clearAutomatically 금지 — AccountWithdrawalService.deletePersonalData Javadoc 참조
    @Modifying
    @Query("DELETE FROM SocialAccount sa WHERE sa.userId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
