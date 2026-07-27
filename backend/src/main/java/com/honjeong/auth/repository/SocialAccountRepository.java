package com.honjeong.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.honjeong.auth.domain.Provider;
import com.honjeong.auth.domain.SocialAccount;

/**
 * 1. 기능: 소셜 계정 연동 매핑의 저장·조회 데이터 접근 (대상 테이블: social_accounts)
 *
 * <p>[기존 주석] SocialAccount 영속성 접근. 소셜 로그인 콜백에서 (공급자, 공급자 사용자 id)로 기존 연동 회원을 찾는다.
 */
public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    /**
     * 기능: 공급자+공급자 사용자 id 조합으로 기존 연동 계정이 있는지 조회(소셜 로그인 신규/재방문 분기)
     * 쿼리: SELECT * FROM social_accounts WHERE provider = :provider AND provider_user_id = :providerUserId
     * Request: provider — 소셜 공급자(KAKAO/APPLE), providerUserId — 공급자 내 사용자 식별자 / Response: Optional&lt;SocialAccount&gt; — 연동 계정(없으면 empty)
     *
     * <p>[기존 주석] 공급자 + 공급자 사용자 id 조합으로 연동 계정을 조회: WHERE provider = ? AND provider_user_id = ?.
     * (이 조합에 UNIQUE 제약이 있어 단건) 결과가 있으면 기존 회원 로그인, 없으면 신규 가입 분기로 처리한다.
     */
    Optional<SocialAccount> findByProviderAndProviderUserId(Provider provider, String providerUserId);

    /**
     * 기능: 사용자의 소셜 계정 연동을 전부 삭제(탈퇴 시 재가입 경로 확보용)
     * 쿼리: DELETE FROM social_accounts WHERE user_id = :userId
     * Request: userId — 대상 사용자 ID / Response: int — 삭제된 행 수
     */
    @Modifying
    @Query("DELETE FROM SocialAccount sa WHERE sa.userId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
