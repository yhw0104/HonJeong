package com.honjeong.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.honjeong.auth.domain.RefreshToken;

/**
 * 리프레시 토큰(해시)의 저장·조회 데이터 접근. (대상 테이블: refresh_tokens)
 *
 * <p>기본 CRUD는 JpaRepository에서 제공받고, 토큰 해시로 단건을 찾는 파생 쿼리만 추가한다.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * 토큰 해시로 저장된 리프레시 토큰을 단건 조회한다(재발급·로그아웃 시 대조용).
     *
     * <p>클라이언트가 보낸 refresh 토큰을 해시해 이 메서드로 찾은 뒤 사용 가능/회수 여부를 검증한다.
     *
     * @param tokenHash refresh 원문의 SHA-256 해시
     * @return 저장된 토큰(없으면 빈 Optional)
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * 사용자의 리프레시 토큰을 전부 삭제한다(탈퇴 시 세션 즉시 무효화용).
     *
     * @param userId 대상 사용자 ID
     * @return 삭제된 행 수
     */
    // clearAutomatically 금지 — AccountWithdrawalService.deletePersonalData Javadoc 참조
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.userId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
