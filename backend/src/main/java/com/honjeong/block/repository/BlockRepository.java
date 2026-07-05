package com.honjeong.block.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.honjeong.block.domain.Block;

public interface BlockRepository extends JpaRepository<Block, Long> {

    /** 두 유저 사이에 차단(어느 방향이든)이 존재하는지 — 상호 은닉 판정의 단일 출처. */
    @Query("""
            SELECT COUNT(b) > 0 FROM Block b
            WHERE (b.blocker.id = :a AND b.blocked.id = :b)
               OR (b.blocker.id = :b AND b.blocked.id = :a)
            """)
    boolean existsBlockBetween(@Param("a") Long a, @Param("b") Long b);

    /** 내가 차단했거나 나를 차단한 상대 id 합집합(목록 필터 원본). */
    @Query("""
            SELECT CASE WHEN b.blocker.id = :userId THEN b.blocked.id ELSE b.blocker.id END
            FROM Block b
            WHERE b.blocker.id = :userId OR b.blocked.id = :userId
            """)
    List<Long> findCounterpartIds(@Param("userId") Long userId);

    /**
     * 목록 필터용 제외 id. 빈 리스트면 JPQL {@code IN ()} 오류가 나므로
     * 어떤 실제 id와도 매칭되지 않는 센티널 {@code -1L}을 넣어 항상 non-empty를 보장한다.
     */
    default List<Long> findExclusionIds(Long userId) {
        List<Long> ids = findCounterpartIds(userId);
        return ids.isEmpty() ? List.of(-1L) : ids;
    }

    Optional<Block> findByBlocker_IdAndBlocked_Id(Long blockerId, Long blockedId);

    /** 내 차단 목록(차단당한 유저 fetch join, 최신순). */
    @Query("""
            SELECT b FROM Block b JOIN FETCH b.blocked
            WHERE b.blocker.id = :userId
            ORDER BY b.createdAt DESC
            """)
    List<Block> findAllWithBlockedByBlocker(@Param("userId") Long userId);
}
