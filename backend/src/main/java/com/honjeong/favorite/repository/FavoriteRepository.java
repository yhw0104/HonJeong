package com.honjeong.favorite.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.honjeong.favorite.domain.Favorite;

/**
 * 1. 기능: 개별 즐겨찾기(그룹-장소 매핑)의 존재 확인·담기/빼기·집계 데이터 접근 (대상 테이블: favorites)
 */
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    /**
     * 기능: 그룹에 해당 장소가 이미 즐겨찾기돼 있는지 확인(담기 멱등 처리용)
     * 쿼리: SELECT COUNT(*) > 0 FROM favorites WHERE group_id = :groupId AND place_id = :placeId
     * Request: groupId — 그룹 ID, placeId — 장소 ID / Response: boolean — 존재 여부
     */
    boolean existsByGroup_IdAndPlace_Id(Long groupId, Long placeId);

    /**
     * 기능: 그룹에서 특정 장소의 즐겨찾기를 삭제(그룹에서 장소 빼기)
     * 쿼리: DELETE FROM favorites WHERE group_id = :groupId AND place_id = :placeId
     * Request: groupId — 그룹 ID, placeId — 뺄 장소 ID / Response: 없음(void)
     */
    void deleteByGroup_IdAndPlace_Id(Long groupId, Long placeId);

    /**
     * 기능: 그룹에 속한 즐겨찾기 전부 삭제(그룹 삭제 시 선행 정리)
     * 쿼리: DELETE FROM favorites WHERE group_id = :groupId
     * Request: groupId — 그룹 ID / Response: 없음(void)
     */
    void deleteByGroup_Id(Long groupId);

    /**
     * 기능: 그룹에 담긴 장소(즐겨찾기) 수 집계
     * 쿼리: SELECT COUNT(*) FROM favorites WHERE group_id = :groupId
     * Request: groupId — 그룹 ID / Response: long — 담긴 즐겨찾기 수
     */
    long countByGroup_Id(Long groupId);

    /**
     * 기능: 그룹에 담긴 즐겨찾기 목록을 장소와 함께 최신 담김 순으로 조회(JOIN FETCH로 N+1 방지)
     * 쿼리: SELECT f.*, p.* FROM favorites f JOIN places p ON p.id = f.place_id
     *       WHERE f.group_id = :groupId ORDER BY f.created_at DESC — place를 즉시 로딩해 그룹 상세 응답에 사용
     * Request: groupId — 그룹 ID / Response: List&lt;Favorite&gt; — place가 로딩된 즐겨찾기 목록
     */
    @Query("SELECT f FROM Favorite f JOIN FETCH f.place WHERE f.group.id = :groupId ORDER BY f.createdAt DESC")
    List<Favorite> findWithPlaceByGroupId(@Param("groupId") Long groupId);

    /**
     * 기능: 사용자가 즐겨찾기한 서로 다른 장소 수 집계(여러 그룹에 겹쳐 담긴 장소는 1개로 — 프로필 활동 통계용, UserActivityService에서 호출)
     * 쿼리: SELECT COUNT(DISTINCT f.place_id) FROM favorites f
     *       JOIN favorite_groups g ON g.id = f.group_id WHERE g.user_id = :userId
     * Request: userId — 사용자 ID / Response: long — 중복 제거된 즐겨찾기 장소 수
     */
    @Query("SELECT COUNT(DISTINCT f.place.id) FROM Favorite f WHERE f.group.user.id = :userId")
    long countDistinctPlaceByUserId(@Param("userId") Long userId);

    /**
     * 기능: 사용자의 그룹들 중 해당 장소가 담겨 있는 그룹 ID 목록 조회(즐겨찾기 상태 표시용)
     * 쿼리: SELECT f.group_id FROM favorites f
     *       JOIN favorite_groups g ON g.id = f.group_id WHERE g.user_id = :userId AND f.place_id = :placeId
     * Request: userId — 사용자 ID, placeId — 장소 ID / Response: List&lt;Long&gt; — 장소를 담고 있는 그룹 ID 목록
     */
    @Query("SELECT f.group.id FROM Favorite f WHERE f.group.user.id = :userId AND f.place.id = :placeId")
    List<Long> findGroupIdsContaining(@Param("userId") Long userId, @Param("placeId") Long placeId);
}
