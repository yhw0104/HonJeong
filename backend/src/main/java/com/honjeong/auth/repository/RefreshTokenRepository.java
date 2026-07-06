package com.honjeong.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.honjeong.auth.domain.RefreshToken;

/**
 * 1. 기능: 리프레시 토큰(해시)의 저장·조회 데이터 접근 (대상 테이블: refresh_tokens)
 *
 * <p>[기존 주석] RefreshToken 영속성 접근. 기본 CRUD는 JpaRepository에서 제공받고,
 * 토큰 해시로 단건을 찾는 파생 쿼리만 추가한다.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * 기능: 토큰 해시로 저장된 리프레시 토큰 단건 조회(재발급·로그아웃 시 대조용)
     * 쿼리: SELECT * FROM refresh_tokens WHERE token_hash = :tokenHash
     * Request: tokenHash — refresh 원문의 SHA-256 해시 / Response: Optional&lt;RefreshToken&gt; — 저장 토큰(없으면 empty)
     *
     * <p>[기존 주석] 토큰 해시로 저장된 토큰을 조회: WHERE token_hash = ?.
     * 클라이언트가 보낸 refresh 토큰을 해시해 이 메서드로 찾은 뒤 사용 가능/회수 여부를 검증한다.
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);
}
