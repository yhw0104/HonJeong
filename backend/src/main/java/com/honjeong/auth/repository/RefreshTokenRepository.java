package com.honjeong.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.honjeong.auth.domain.RefreshToken;

/**
 * RefreshToken 영속성 접근. 기본 CRUD는 JpaRepository에서 제공받고,
 * 토큰 해시로 단건을 찾는 파생 쿼리만 추가한다.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * 토큰 해시로 저장된 토큰을 조회: WHERE token_hash = ?.
     * 클라이언트가 보낸 refresh 토큰을 해시해 이 메서드로 찾은 뒤 사용 가능/회수 여부를 검증한다.
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);
}
