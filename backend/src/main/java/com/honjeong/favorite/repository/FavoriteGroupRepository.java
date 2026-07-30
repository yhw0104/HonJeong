package com.honjeong.favorite.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.honjeong.favorite.domain.FavoriteGroup;
import com.honjeong.favorite.dto.FavoriteGroupSummaryResponse;

/**
 * 즐겨찾기 그룹의 조회·기본 그룹 존재 확인·요약(장소 수 포함) 집계 데이터 접근 (대상 테이블: favorite_groups).
 */
public interface FavoriteGroupRepository extends JpaRepository<FavoriteGroup, Long> {

    /**
     * 사용자가 기본 그룹을 이미 보유하는지 확인(가입 시 기본 그룹 자동 생성의 멱등 처리용).
     *
     * @param userId 사용자 ID
     * @return 기본 그룹 존재 여부
     */
    boolean existsByUser_IdAndIsDefaultTrue(Long userId);

    /**
     * 사용자의 그룹 목록을 생성 순으로 조회(즐겨찾기 상태 조회에서 전체 그룹 나열용).
     *
     * @param userId 사용자 ID
     * @return 생성 순 그룹 목록
     */
    List<FavoriteGroup> findByUser_IdOrderByCreatedAtAsc(Long userId);

    /**
     * 사용자의 그룹 목록을 담긴 장소 수 포함 요약 DTO로 한 번에 조회(생성 순 정렬, 빈 그룹은 LEFT JOIN으로 0건 포함).
     *
     * @param userId 사용자 ID
     * @return 그룹 요약(장소 수 포함) 목록
     */
    @Query("""
            SELECT new com.honjeong.favorite.dto.FavoriteGroupSummaryResponse(
                   g.id, g.name, g.note, g.color, g.isDefault, COUNT(f.id))
            FROM FavoriteGroup g LEFT JOIN g.favorites f
            WHERE g.user.id = :userId
            GROUP BY g.id, g.name, g.note, g.color, g.isDefault, g.createdAt
            ORDER BY g.createdAt ASC""")
    List<FavoriteGroupSummaryResponse> findSummaries(@Param("userId") Long userId);

    /**
     * 사용자의 즐겨찾기 그룹을 전부 삭제(탈퇴 시 개인정보 정리용) — favorites는 DB FK ON DELETE CASCADE로 함께 삭제된다.
     *
     * @param userId 대상 사용자 ID
     * @return 삭제된 행 수
     */
    // clearAutomatically 금지 — AccountWithdrawalService.deletePersonalData Javadoc 참조
    @Modifying
    @Query("DELETE FROM FavoriteGroup fg WHERE fg.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
