package com.honjeong.block.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.honjeong.block.domain.Block;

/**
 * 유저 차단 관계 데이터 접근 — 차단 저장/삭제·양방향 차단 판정·목록 필터용 제외 ID 제공 (대상 테이블: blocks).
 */
public interface BlockRepository extends JpaRepository<Block, Long> {

    /**
     * 두 유저 사이에 방향 무관 차단이 하나라도 존재하는지 확인.
     * <p>두 유저 사이에 차단(어느 방향이든)이 존재하는지 — 상호 은닉 판정의 단일 출처.
     *
     * @param a 유저 A ID
     * @param b 유저 B ID
     * @return 차단 존재 여부
     */
    @Query("""
            SELECT COUNT(b) > 0 FROM Block b
            WHERE (b.blocker.id = :a AND b.blocked.id = :b)
               OR (b.blocker.id = :b AND b.blocked.id = :a)
            """)
    boolean existsBlockBetween(@Param("a") Long a, @Param("b") Long b);

    /**
     * 내가 차단했거나 나를 차단한 상대 유저 ID 합집합 조회.
     * <p>내가 차단했거나 나를 차단한 상대 id 합집합(목록 필터 원본).
     *
     * @param userId 기준 유저 ID
     * @return 차단 관계 상대 유저 ID 목록(없으면 빈 리스트)
     */
    @Query("""
            SELECT CASE WHEN b.blocker.id = :userId THEN b.blocked.id ELSE b.blocker.id END
            FROM Block b
            WHERE b.blocker.id = :userId OR b.blocked.id = :userId
            """)
    List<Long> findCounterpartIds(@Param("userId") Long userId);

    /**
     * 목록 쿼리의 NOT IN 필터에 바로 쓸 제외 유저 ID 목록 조회(항상 non-empty 보장).
     * <p>목록 필터용 제외 id. 빈 리스트면 JPQL {@code IN ()} 오류가 나므로
     * 어떤 실제 id와도 매칭되지 않는 센티널 {@code -1L}을 넣어 항상 non-empty를 보장한다.
     *
     * @param userId 기준 유저 ID
     * @return 제외할 유저 ID 목록(비어 있으면 [-1L])
     */
    default List<Long> findExclusionIds(Long userId) {
        List<Long> ids = findCounterpartIds(userId);
        return ids.isEmpty() ? List.of(-1L) : ids;
    }

    /**
     * 특정 방향(blocker→blocked)의 차단 단건 조회 — 차단 해제 시 대상 확인용.
     *
     * @param blockerId 차단한 유저 ID
     * @param blockedId 차단당한 유저 ID
     * @return 차단 엔티티(없으면 empty)
     */
    Optional<Block> findByBlocker_IdAndBlocked_Id(Long blockerId, Long blockedId);

    /**
     * 내가 차단한 목록을 차단당한 유저 정보와 함께 최신순으로 조회.
     * <p>내 차단 목록(차단당한 유저 fetch join, 최신순).
     *
     * @param userId 차단한 유저 ID
     * @return 차단 엔티티 목록(blocked 유저 즉시 로딩 완료)
     */
    @Query("""
            SELECT b FROM Block b JOIN FETCH b.blocked
            WHERE b.blocker.id = :userId
            ORDER BY b.createdAt DESC
            """)
    List<Block> findAllWithBlockedByBlocker(@Param("userId") Long userId);

    /**
     * 사용자가 관련된(차단했거나 차단당한) 차단 관계를 전부 삭제(탈퇴 시 관계 정리용).
     *
     * @param userId 대상 사용자 ID
     * @return 삭제된 행 수
     */
    // clearAutomatically 금지 — AccountWithdrawalService.deletePersonalData Javadoc 참조
    @Modifying
    @Query("DELETE FROM Block b WHERE b.blocker.id = :userId OR b.blocked.id = :userId")
    int deleteAllInvolvingUser(@Param("userId") Long userId);
}
