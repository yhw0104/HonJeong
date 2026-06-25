package com.honjeong.review.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.honjeong.review.domain.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query("""
            SELECT DISTINCT r FROM Review r
            JOIN FETCH r.user
            LEFT JOIN FETCH r.tags
            WHERE r.place.id = :placeId
            ORDER BY r.createdAt DESC
            """)
    List<Review> findByPlaceWithUserAndTags(@Param("placeId") Long placeId);
}
