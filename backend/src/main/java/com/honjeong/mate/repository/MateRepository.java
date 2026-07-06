package com.honjeong.mate.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.honjeong.mate.domain.Mate;

/**
 * 1. 기능: 혼밥 메이트(친구) 관계 데이터 접근 (대상 테이블: mates)
 */
public interface MateRepository extends JpaRepository<Mate, Long> {

    /**
     * 기능: 두 사용자가 이미 메이트 관계(user→mateUser 방향)인지 확인
     * 쿼리: SELECT COUNT(*) > 0 FROM mates WHERE user_id = :userId AND mate_user_id = :mateUserId
     * Request: userId — 주체 사용자 ID, mateUserId — 상대 사용자 ID / Response: boolean — 존재 여부
     */
    boolean existsByUser_IdAndMateUser_Id(Long userId, Long mateUserId);

    /**
     * 기능: 사용자의 메이트 수 집계 (더보기 활동 요약용)
     * 쿼리: SELECT COUNT(*) FROM mates WHERE user_id = :userId
     * Request: userId — 사용자 ID / Response: long — 메이트 수
     */
    long countByUser_Id(Long userId);

    /**
     * 기능: 내 메이트 목록을 상대 사용자 정보와 함께 최신순 조회 (JOIN FETCH로 mateUser 즉시 로딩 — N+1 방지)
     * 쿼리: SELECT m.*, u.* FROM mates m JOIN users u ON u.id = m.mate_user_id WHERE m.user_id = :userId ORDER BY m.created_at DESC
     * Request: userId — 사용자 ID / Response: List<Mate> — 메이트 관계 목록(mateUser 로딩된 상태)
     */
    @Query("""
            SELECT m FROM Mate m
            JOIN FETCH m.mateUser
            WHERE m.user.id = :userId
            ORDER BY m.createdAt DESC
            """)
    List<Mate> findMatesWithUserByUserId(@Param("userId") Long userId);

    /**
     * 기능: 특정 방향(user→mateUser)의 메이트 관계 단건 조회 (메이트 해제·차단 자동 정리용)
     * 쿼리: SELECT * FROM mates WHERE user_id = :userId AND mate_user_id = :mateUserId
     * Request: userId — 주체 사용자 ID, mateUserId — 상대 사용자 ID / Response: Optional<Mate> — 관계(없으면 empty)
     */
    Optional<Mate> findByUser_IdAndMateUser_Id(Long userId, Long mateUserId);
}
