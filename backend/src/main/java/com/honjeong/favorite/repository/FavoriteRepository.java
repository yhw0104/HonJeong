package com.honjeong.favorite.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.honjeong.favorite.domain.Favorite;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    boolean existsByGroup_IdAndPlace_Id(Long groupId, Long placeId);

    void deleteByGroup_IdAndPlace_Id(Long groupId, Long placeId);

    void deleteByGroup_Id(Long groupId);

    long countByGroup_Id(Long groupId);
}
