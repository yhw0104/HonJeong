package com.honjeong.favorite.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.honjeong.favorite.domain.FavoriteGroup;
import com.honjeong.favorite.dto.FavoriteGroupSummaryResponse;

public interface FavoriteGroupRepository extends JpaRepository<FavoriteGroup, Long> {

    boolean existsByUser_IdAndIsDefaultTrue(Long userId);

    List<FavoriteGroup> findByUser_IdOrderByCreatedAtAsc(Long userId);

    @Query("""
            SELECT new com.honjeong.favorite.dto.FavoriteGroupSummaryResponse(
                   g.id, g.name, g.note, g.color, g.isDefault, COUNT(f.id))
            FROM FavoriteGroup g LEFT JOIN g.favorites f
            WHERE g.user.id = :userId
            GROUP BY g.id, g.name, g.note, g.color, g.isDefault, g.createdAt
            ORDER BY g.createdAt ASC""")
    List<FavoriteGroupSummaryResponse> findSummaries(@Param("userId") Long userId);
}
