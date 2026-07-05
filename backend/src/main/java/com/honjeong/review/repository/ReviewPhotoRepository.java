package com.honjeong.review.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.honjeong.review.domain.ReviewPhoto;

public interface ReviewPhotoRepository extends JpaRepository<ReviewPhoto, Long> {

    /** placeId의 모든 리뷰 사진을 리뷰 최신순(visitedAt DESC, id DESC) + sortOrder ASC로 평탄화. */
    @Query("""
            SELECT p.review.id AS reviewId, p.imageUrl AS imageUrl
            FROM ReviewPhoto p
            WHERE p.review.place.id = :placeId
            ORDER BY p.review.visitedAt DESC, p.review.id DESC, p.sortOrder ASC
            """)
    List<ReviewPhotoRow> findByPlaceFlattened(@Param("placeId") Long placeId);

    /** userId의 모든 리뷰 사진을 리뷰 최신순(createdAt DESC, id DESC) + sortOrder ASC로 평탄화. */
    @Query("""
            SELECT p.review.id AS reviewId, p.imageUrl AS imageUrl
            FROM ReviewPhoto p
            WHERE p.review.user.id = :userId
            ORDER BY p.review.createdAt DESC, p.review.id DESC, p.sortOrder ASC
            """)
    List<ReviewPhotoRow> findByUserFlattened(@Param("userId") Long userId);

    /** 평탄화 사진 한 행(리뷰 id + 사진 url). */
    interface ReviewPhotoRow {
        Long getReviewId();
        String getImageUrl();
    }
}
