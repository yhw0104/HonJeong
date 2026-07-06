package com.honjeong.favorite.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.honjeong.favorite.domain.FavoriteGroup;
import com.honjeong.favorite.dto.FavoriteGroupSummaryResponse;

/**
 * 1. 기능: 즐겨찾기 그룹의 조회·기본 그룹 존재 확인·요약(장소 수 포함) 집계 데이터 접근 (대상 테이블: favorite_groups)
 */
public interface FavoriteGroupRepository extends JpaRepository<FavoriteGroup, Long> {

    /**
     * 기능: 사용자가 기본 그룹을 이미 보유하는지 확인(가입 시 기본 그룹 자동 생성의 멱등 처리용)
     * 쿼리: SELECT COUNT(*) > 0 FROM favorite_groups WHERE user_id = :userId AND is_default = true
     * Request: userId — 사용자 ID / Response: boolean — 기본 그룹 존재 여부
     */
    boolean existsByUser_IdAndIsDefaultTrue(Long userId);

    /**
     * 기능: 사용자의 그룹 목록을 생성 순으로 조회(즐겨찾기 상태 조회에서 전체 그룹 나열용)
     * 쿼리: SELECT * FROM favorite_groups WHERE user_id = :userId ORDER BY created_at ASC
     * Request: userId — 사용자 ID / Response: List&lt;FavoriteGroup&gt; — 생성 순 그룹 목록
     */
    List<FavoriteGroup> findByUser_IdOrderByCreatedAtAsc(Long userId);

    /**
     * 기능: 사용자의 그룹 목록을 담긴 장소 수 포함 요약 DTO로 한 번에 조회(생성 순 정렬, 빈 그룹은 LEFT JOIN으로 0건 포함)
     * 쿼리: SELECT g.id, g.name, g.note, g.color, g.is_default, COUNT(f.id) FROM favorite_groups g
     *       LEFT JOIN favorites f ON f.group_id = g.id WHERE g.user_id = :userId
     *       GROUP BY g.id, g.name, g.note, g.color, g.is_default, g.created_at ORDER BY g.created_at ASC
     *       — 결과를 new 생성자 프로젝션으로 FavoriteGroupSummaryResponse에 바로 매핑
     * Request: userId — 사용자 ID / Response: List&lt;FavoriteGroupSummaryResponse&gt; — 그룹 요약(장소 수 포함) 목록
     */
    @Query("""
            SELECT new com.honjeong.favorite.dto.FavoriteGroupSummaryResponse(
                   g.id, g.name, g.note, g.color, g.isDefault, COUNT(f.id))
            FROM FavoriteGroup g LEFT JOIN g.favorites f
            WHERE g.user.id = :userId
            GROUP BY g.id, g.name, g.note, g.color, g.isDefault, g.createdAt
            ORDER BY g.createdAt ASC""")
    List<FavoriteGroupSummaryResponse> findSummaries(@Param("userId") Long userId);
}
