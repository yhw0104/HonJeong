package com.honjeong.favorite.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.honjeong.favorite.domain.Favorite;

/**
 * 개별 즐겨찾기(그룹-장소 매핑)의 존재 확인·담기/빼기·집계 데이터 접근 (대상 테이블: favorites).
 */
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    /**
     * 그룹에 해당 장소가 이미 즐겨찾기돼 있는지 확인(담기 멱등 처리용).
     *
     * @param groupId 그룹 ID
     * @param placeId 장소 ID
     * @return 존재 여부
     */
    boolean existsByGroup_IdAndPlace_Id(Long groupId, Long placeId);

    /**
     * 그룹에서 특정 장소의 즐겨찾기를 삭제(그룹에서 장소 빼기).
     *
     * @param groupId 그룹 ID
     * @param placeId 뺄 장소 ID
     */
    void deleteByGroup_IdAndPlace_Id(Long groupId, Long placeId);

    /**
     * 그룹에 속한 즐겨찾기 전부 삭제(그룹 삭제 시 선행 정리).
     *
     * @param groupId 그룹 ID
     */
    void deleteByGroup_Id(Long groupId);

    /**
     * 그룹에 담긴 장소(즐겨찾기) 수 집계.
     *
     * @param groupId 그룹 ID
     * @return 담긴 즐겨찾기 수
     */
    long countByGroup_Id(Long groupId);

    /**
     * 그룹에 담긴 즐겨찾기 목록을 장소와 함께 최신 담김 순으로 조회(JOIN FETCH로 N+1 방지).
     *
     * @param groupId 그룹 ID
     * @return place가 로딩된 즐겨찾기 목록
     */
    @Query("SELECT f FROM Favorite f JOIN FETCH f.place WHERE f.group.id = :groupId ORDER BY f.createdAt DESC")
    List<Favorite> findWithPlaceByGroupId(@Param("groupId") Long groupId);

    /**
     * 사용자가 즐겨찾기한 서로 다른 장소 수 집계(여러 그룹에 겹쳐 담긴 장소는 1개로 — 프로필 활동 통계용, UserActivityService에서 호출).
     *
     * @param userId 사용자 ID
     * @return 중복 제거된 즐겨찾기 장소 수
     */
    @Query("SELECT COUNT(DISTINCT f.place.id) FROM Favorite f WHERE f.group.user.id = :userId")
    long countDistinctPlaceByUserId(@Param("userId") Long userId);

    /**
     * 사용자의 그룹들 중 해당 장소가 담겨 있는 그룹 ID 목록 조회(즐겨찾기 상태 표시용).
     *
     * @param userId 사용자 ID
     * @param placeId 장소 ID
     * @return 장소를 담고 있는 그룹 ID 목록
     */
    @Query("SELECT f.group.id FROM Favorite f WHERE f.group.user.id = :userId AND f.place.id = :placeId")
    List<Long> findGroupIdsContaining(@Param("userId") Long userId, @Param("placeId") Long placeId);

    /**
     * 이 장소를 즐겨찾기한 서로 다른 사용자 수(사회적 증거 "N명이 저장") — 여러 그룹 중복은 1명으로.
     *
     * @param placeId 장소 ID
     * @return 중복 제거된 저장자 수
     */
    @Query("SELECT COUNT(DISTINCT f.group.user.id) FROM Favorite f WHERE f.place.id = :placeId")
    long countDistinctSaversByPlace(@Param("placeId") Long placeId);

    /**
     * 내 메이트 중 이 장소를 즐겨찾기한 사용자 목록(사회적 증거 "내 메이트 M명 포함" + 아바타 스택) 한 사람이 여러 그룹에 담아도 1명으로(GROUP BY) — userId 오름차순 결정적 정렬.
     *
     * @param placeId 장소 ID
     * @param mateIds 내 메이트 ID 목록
     * @return 저장한 메이트(프로필 포함) 목록
     */
    @Query("SELECT u.id AS userId, u.nickname AS nickname, u.profileImageUrl AS profileImageUrl "
            + "FROM Favorite f JOIN f.group g JOIN g.user u "
            + "WHERE f.place.id = :placeId AND u.id IN :mateIds "
            + "GROUP BY u.id, u.nickname, u.profileImageUrl ORDER BY u.id")
    List<SaverMateRow> findSaverMatesByPlace(@Param("placeId") Long placeId, @Param("mateIds") Collection<Long> mateIds);

    /** 저장한 메이트 1명의 아바타 표시 필드(프로젝션). */
    interface SaverMateRow {
        Long getUserId();
        String getNickname();
        String getProfileImageUrl();
    }
}
