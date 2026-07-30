package com.honjeong.review.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.honjeong.review.domain.ReviewPhoto;

/**
 * 리뷰 사진 데이터 접근 — 식당별/사용자별 사진 평탄화 조회. (대상 테이블: review_photos)
 */
public interface ReviewPhotoRepository extends JpaRepository<ReviewPhoto, Long> {

    /**
     * 식당의 모든 리뷰 사진을 리뷰 최신순(visitedAt DESC, id DESC) + sortOrder ASC로 평탄화 조회한다.
     *
     * @param placeId 식당 ID
     * @return (리뷰 ID, 사진 URL) 행 목록
     */
    @Query("""
            SELECT p.review.id AS reviewId, p.imageUrl AS imageUrl
            FROM ReviewPhoto p
            WHERE p.review.place.id = :placeId
            ORDER BY p.review.visitedAt DESC, p.review.id DESC, p.sortOrder ASC
            """)
    List<ReviewPhotoRow> findByPlaceFlattened(@Param("placeId") Long placeId);

    /**
     * 사용자의 모든 리뷰 사진을 리뷰 최신순(createdAt DESC, id DESC) + sortOrder ASC로 평탄화 조회한다.
     *
     * @param userId 사용자 ID
     * @return (리뷰 ID, 사진 URL) 행 목록
     */
    @Query("""
            SELECT p.review.id AS reviewId, p.imageUrl AS imageUrl
            FROM ReviewPhoto p
            WHERE p.review.user.id = :userId
            ORDER BY p.review.createdAt DESC, p.review.id DESC, p.sortOrder ASC
            """)
    List<ReviewPhotoRow> findByUserFlattened(@Param("userId") Long userId);

    /**
     * 여러 식당의 리뷰 사진을 식당별 리뷰 최신순으로 한 번에 평탄화 조회한다 — 주변 목록 썸네일용 배치 조회.
     *
     * <p>정렬은 place_id ASC → 리뷰 최신순 → sortOrder ASC이며, 식당별 상한 N장 절단은 호출 측 책임이다.
     *
     * @param placeIds 식당 ID 목록(빈 목록 금지)
     * @return (식당 ID, 사진 URL) 행 목록
     */
    @Query("""
            SELECT p.review.place.id AS placeId, p.imageUrl AS imageUrl
            FROM ReviewPhoto p
            WHERE p.review.place.id IN :placeIds
            ORDER BY p.review.place.id ASC, p.review.visitedAt DESC, p.review.id DESC, p.sortOrder ASC
            """)
    List<PlacePhotoRow> findByPlaceIdsFlattened(@Param("placeIds") List<Long> placeIds);

    /**
     * 배치 평탄화 조회 결과 한 행(식당 ID + 사진 URL)을 나타내는 프로젝션.
     */
    interface PlacePhotoRow {
        /** 사진이 속한 식당 ID */
        Long getPlaceId();
        /** 사진 이미지 URL */
        String getImageUrl();
    }

    /**
     * 평탄화 조회 결과 한 행(리뷰 ID + 사진 URL)을 나타내는 프로젝션.
     */
    interface ReviewPhotoRow {
        /** 사진이 속한 리뷰 ID */
        Long getReviewId();
        /** 사진 이미지 URL */
        String getImageUrl();
    }
}
