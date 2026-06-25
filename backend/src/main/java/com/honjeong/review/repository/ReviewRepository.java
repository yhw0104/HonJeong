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

    @Query("""
            SELECT AVG(r.tasteRating), AVG(r.soloFriendlyRating), COUNT(r)
            FROM Review r WHERE r.place.id = :placeId
            """)
    List<Object[]> summarizeByPlace(@Param("placeId") Long placeId);

    @Query("""
            SELECT rt.tag, COUNT(rt) FROM ReviewTag rt
            WHERE rt.place.id = :placeId
            GROUP BY rt.tag
            ORDER BY COUNT(rt) DESC
            """)
    List<Object[]> countTagsByPlace(@Param("placeId") Long placeId);
}
