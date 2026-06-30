package com.honjeong.favorite.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.honjeong.favorite.domain.Favorite;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByGroup_IdAndPlace_Id(Long groupId, Long placeId);

    void deleteByGroup_IdAndPlace_Id(Long groupId, Long placeId);

    void deleteByGroup_Id(Long groupId);

    long countByGroup_Id(Long groupId);

    @Query("SELECT f FROM Favorite f JOIN FETCH f.place WHERE f.group.id = :groupId ORDER BY f.createdAt DESC")
    List<Favorite> findWithPlaceByGroupId(@Param("groupId") Long groupId);

    @Query("SELECT COUNT(DISTINCT f.place.id) FROM Favorite f WHERE f.group.user.id = :userId")
    long countDistinctPlaceByUserId(@Param("userId") Long userId);

    @Query("SELECT f.group.id FROM Favorite f WHERE f.group.user.id = :userId AND f.place.id = :placeId")
    List<Long> findGroupIdsContaining(@Param("userId") Long userId, @Param("placeId") Long placeId);
}
