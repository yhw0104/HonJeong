package com.honjeong.review.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.honjeong.review.domain.ReviewPhoto;

/**
 * 1. 기능: 리뷰 사진 데이터 접근 — 식당별/사용자별 사진 평탄화 조회 (대상 테이블: review_photos)
 */
public interface ReviewPhotoRepository extends JpaRepository<ReviewPhoto, Long> {

    /**
     * 기능: 식당의 모든 리뷰 사진을 리뷰 최신순으로 평탄화 조회
     * 쿼리: SELECT p.review_id, p.image_url FROM review_photos p JOIN reviews r ON p.review_id = r.id WHERE r.place_id = :placeId ORDER BY r.visited_at DESC, r.id DESC, p.sort_order ASC
     * Request: placeId — 식당 ID / Response: List<ReviewPhotoRow> — (리뷰 ID, 사진 URL) 행 목록
     *
     * <p>[기존 주석] placeId의 모든 리뷰 사진을 리뷰 최신순(visitedAt DESC, id DESC) + sortOrder ASC로 평탄화.
     */
    @Query("""
            SELECT p.review.id AS reviewId, p.imageUrl AS imageUrl
            FROM ReviewPhoto p
            WHERE p.review.place.id = :placeId
            ORDER BY p.review.visitedAt DESC, p.review.id DESC, p.sortOrder ASC
            """)
    List<ReviewPhotoRow> findByPlaceFlattened(@Param("placeId") Long placeId);

    /**
     * 기능: 사용자의 모든 리뷰 사진을 리뷰 작성 최신순으로 평탄화 조회
     * 쿼리: SELECT p.review_id, p.image_url FROM review_photos p JOIN reviews r ON p.review_id = r.id WHERE r.user_id = :userId ORDER BY r.created_at DESC, r.id DESC, p.sort_order ASC
     * Request: userId — 사용자 ID / Response: List<ReviewPhotoRow> — (리뷰 ID, 사진 URL) 행 목록
     *
     * <p>[기존 주석] userId의 모든 리뷰 사진을 리뷰 최신순(createdAt DESC, id DESC) + sortOrder ASC로 평탄화.
     */
    @Query("""
            SELECT p.review.id AS reviewId, p.imageUrl AS imageUrl
            FROM ReviewPhoto p
            WHERE p.review.user.id = :userId
            ORDER BY p.review.createdAt DESC, p.review.id DESC, p.sortOrder ASC
            """)
    List<ReviewPhotoRow> findByUserFlattened(@Param("userId") Long userId);

    /**
     * 평탄화 조회 결과 한 행(리뷰 ID + 사진 URL)을 나타내는 프로젝션.
     *
     * <p>[기존 주석] 평탄화 사진 한 행(리뷰 id + 사진 url).
     */
    interface ReviewPhotoRow {
        /** 사진이 속한 리뷰 ID */
        Long getReviewId();
        /** 사진 이미지 URL */
        String getImageUrl();
    }
}
