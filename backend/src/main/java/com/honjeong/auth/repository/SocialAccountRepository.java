package com.honjeong.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.honjeong.auth.domain.Provider;
import com.honjeong.auth.domain.SocialAccount;

/**
 * SocialAccount 영속성 접근. 소셜 로그인 콜백에서 (공급자, 공급자 사용자 id)로 기존 연동 회원을 찾는다.
 */
public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    /**
     * 공급자 + 공급자 사용자 id 조합으로 연동 계정을 조회: WHERE provider = ? AND provider_user_id = ?.
     * (이 조합에 UNIQUE 제약이 있어 단건) 결과가 있으면 기존 회원 로그인, 없으면 신규 가입 분기로 처리한다.
     */
    Optional<SocialAccount> findByProviderAndProviderUserId(Provider provider, String providerUserId);
}
