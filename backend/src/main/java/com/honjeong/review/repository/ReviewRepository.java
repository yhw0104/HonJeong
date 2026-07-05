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
              AND r.user.id NOT IN :excludedUserIds
            ORDER BY r.createdAt DESC
            """)
    List<Review> findByPlaceWithUserAndTags(@Param("placeId") Long placeId,
            @Param("excludedUserIds") List<Long> excludedUserIds);

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

    /**
     * 사용자의 체크인 연결 리뷰 목록. checkIn IS NOT NULL인 리뷰만 태그와 함께 createdAt DESC로 조회한다.
     *
     * @param userId 회원 id
     * @return 체크인 연결 리뷰 목록(tags LEFT JOIN FETCH, 최신순)
     */
    @Query("""
            SELECT DISTINCT r FROM Review r
            LEFT JOIN FETCH r.tags
            WHERE r.user.id = :userId AND r.checkIn IS NOT NULL
            ORDER BY r.createdAt DESC
            """)
    List<Review> findByUserWithCheckIn(@Param("userId") Long userId);

    /**
     * 사용자의 전체 리뷰 수.
     *
     * @param userId 회원 id
     * @return 총 리뷰 건수
     */
    long countByUser_Id(Long userId);

    /**
     * 해당 체크인에 이미 리뷰가 있는지. 한 방문(체크인)=한 리뷰 제약(부분 유니크)의 사전 검사용.
     *
     * @param checkInId 체크인 id
     * @return 그 체크인에 연결된 리뷰 존재 여부
     */
    boolean existsByCheckIn_Id(Long checkInId);
}
