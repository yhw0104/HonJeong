package com.honjeong.mate.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.honjeong.mate.domain.Mate;

/**
 * 혼밥 메이트(친구) 관계 데이터 접근 (대상 테이블: mates).
 */
public interface MateRepository extends JpaRepository<Mate, Long> {

    /**
     * 두 사용자가 이미 메이트 관계(user→mateUser 방향)인지 확인.
     *
     * @param userId 주체 사용자 ID
     * @param mateUserId 상대 사용자 ID
     * @return 존재 여부
     */
    boolean existsByUser_IdAndMateUser_Id(Long userId, Long mateUserId);

    /**
     * 사용자의 메이트 수 집계 (더보기 활동 요약용).
     *
     * @param userId 사용자 ID
     * @return 메이트 수
     */
    long countByUser_Id(Long userId);

    /**
     * 내 메이트 목록을 상대 사용자 정보와 함께 최신순 조회 (JOIN FETCH로 mateUser 즉시 로딩 — N+1 방지).
     *
     * @param userId 사용자 ID
     * @return 메이트 관계 목록(mateUser 로딩된 상태)
     */
    @Query("""
            SELECT m FROM Mate m
            JOIN FETCH m.mateUser
            WHERE m.user.id = :userId
            ORDER BY m.createdAt DESC
            """)
    List<Mate> findMatesWithUserByUserId(@Param("userId") Long userId);

    /**
     * 특정 방향(user→mateUser)의 메이트 관계 단건 조회 (메이트 해제·차단 자동 정리용).
     *
     * @param userId 주체 사용자 ID
     * @param mateUserId 상대 사용자 ID
     * @return 관계(없으면 empty)
     */
    Optional<Mate> findByUser_IdAndMateUser_Id(Long userId, Long mateUserId);

    /**
     * 사용자가 관련된(양방향) 메이트 관계를 전부 삭제(탈퇴 시 관계 정리용).
     *
     * @param userId 대상 사용자 ID
     * @return 삭제된 행 수
     */
    // clearAutomatically 금지 — AccountWithdrawalService.deletePersonalData Javadoc 참조
    @Modifying
    @Query("DELETE FROM Mate m WHERE m.user.id = :userId OR m.mateUser.id = :userId")
    int deleteAllInvolvingUser(@Param("userId") Long userId);
}
