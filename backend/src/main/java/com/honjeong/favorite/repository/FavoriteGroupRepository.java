package com.honjeong.favorite.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.honjeong.favorite.domain.FavoriteGroup;

public interface FavoriteGroupRepository extends JpaRepository<FavoriteGroup, Long> {

    boolean existsByUser_IdAndIsDefaultTrue(Long userId);

    List<FavoriteGroup> findByUser_IdOrderByCreatedAtAsc(Long userId);
}
