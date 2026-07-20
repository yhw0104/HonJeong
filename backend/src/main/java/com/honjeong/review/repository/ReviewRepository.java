package com.honjeong.review.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.honjeong.review.domain.Review;

/**
 * 1. 기능: 리뷰 데이터 접근 — 식당별 목록·집계, 사용자별 목록·카운트 (대상 테이블: reviews, review_tags)
 */
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * 기능: 식당의 리뷰 목록을 작성자·태그와 함께 최신순 조회(차단 관계 사용자 리뷰 제외)
     * 쿼리: SELECT DISTINCT r.* FROM reviews r JOIN users u ON r.user_id = u.id LEFT JOIN review_tags t ON t.review_id = r.id WHERE r.place_id = :placeId AND r.user_id NOT IN (:excludedUserIds) ORDER BY r.created_at DESC — user는 JOIN FETCH, tags는 LEFT JOIN FETCH로 함께 로딩(N+1 방지)
     * Request: placeId — 식당 ID, excludedUserIds — 제외할 사용자 ID 목록(차단 관계) / Response: List<Review> — 리뷰 목록(user·tags 로딩됨)
     */
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

    /**
     * 기능: 식당 리뷰의 별점 평균 2종과 리뷰 수 집계
     * 쿼리: SELECT AVG(taste_rating), AVG(solo_friendly_rating), COUNT(*) FROM reviews WHERE place_id = :placeId
     * Request: placeId — 식당 ID / Response: List<Object[]> — [맛 평균, 혼밥 적합도 평균, 건수] 단일 행
     */
    @Query("""
            SELECT AVG(r.tasteRating), AVG(r.soloFriendlyRating), COUNT(r)
            FROM Review r WHERE r.place.id = :placeId
            """)
    List<Object[]> summarizeByPlace(@Param("placeId") Long placeId);

    /**
     * 기능: 여러 식당의 리뷰 수·별점 평균 2종을 식당별로 한 번에 집계 — 주변 목록 카드용 배치 조회.
     * 쿼리: WHERE r.place_id IN :placeIds GROUP BY r.place_id (리뷰 있는 식당만 행이 나옴 — 없는 식당은 호출측이 0/null 처리)
     * Request: placeIds — 식당 ID 목록(빈 목록 금지) / Response: List&lt;PlaceReviewStatRow&gt; — 식당별 (개수, 맛평균, 혼밥평균)
     */
    @Query("""
            SELECT r.place.id AS placeId, COUNT(r) AS reviewCount,
                   AVG(r.tasteRating) AS avgTaste, AVG(r.soloFriendlyRating) AS avgSolo
            FROM Review r
            WHERE r.place.id IN :placeIds
            GROUP BY r.place.id
            """)
    List<PlaceReviewStatRow> summarizeByPlaceIds(@Param("placeIds") List<Long> placeIds);

    /**
     * 배치 리뷰 집계 한 행(식당 ID + 리뷰 수 + 별점 평균 2종) 프로젝션.
     */
    interface PlaceReviewStatRow {
        /** 집계 대상 식당 ID */
        Long getPlaceId();
        /** 리뷰 수 */
        long getReviewCount();
        /** 맛 별점 평균(리뷰 있으면 non-null) */
        Double getAvgTaste();
        /** 혼밥 적합도 별점 평균(리뷰 있으면 non-null) */
        Double getAvgSolo();
    }

    /**
     * 기능: 식당의 친화 태그별 부착 횟수를 빈도 내림차순으로 집계
     * 쿼리: SELECT tag, COUNT(*) FROM review_tags WHERE place_id = :placeId GROUP BY tag ORDER BY COUNT(*) DESC
     * Request: placeId — 식당 ID / Response: List<Object[]> — [태그, 횟수] 행 목록
     */
    @Query("""
            SELECT rt.tag, COUNT(rt) FROM ReviewTag rt
            WHERE rt.place.id = :placeId
            GROUP BY rt.tag
            ORDER BY COUNT(rt) DESC
            """)
    List<Object[]> countTagsByPlace(@Param("placeId") Long placeId);

    /**
     * 기능: 사용자의 인증(체크인 연결) 리뷰를 태그와 함께 작성 최신순 조회
     * 쿼리: SELECT DISTINCT r.* FROM reviews r LEFT JOIN review_tags t ON t.review_id = r.id WHERE r.user_id = :userId AND r.check_in_id IS NOT NULL ORDER BY r.created_at DESC — tags LEFT JOIN FETCH
     * Request: userId — 사용자 ID / Response: List<Review> — 인증 리뷰 목록(tags 로딩됨)
     *
     * <p>[기존 주석] 사용자의 체크인 연결 리뷰 목록. checkIn IS NOT NULL인 리뷰만 태그와 함께 createdAt DESC로 조회한다.
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
     * 기능: 사용자의 전체 리뷰(인증+일반)를 식당·태그와 함께 작성 최신순 조회
     * 쿼리: SELECT DISTINCT r.* FROM reviews r JOIN places p ON r.place_id = p.id LEFT JOIN review_tags t ON t.review_id = r.id WHERE r.user_id = :userId ORDER BY r.created_at DESC, r.id DESC — place JOIN FETCH, tags LEFT JOIN FETCH
     * Request: userId — 사용자 ID / Response: List<Review> — 내 리뷰 목록(place·tags 로딩됨)
     *
     * <p>[기존 주석] 사용자의 전체 리뷰(인증+일반)를 작성 최신순으로 조회한다. '내가 쓴 리뷰' 화면용.
     * place는 fetch join, tags는 left fetch join(사진은 MultipleBagFetch 회피를 위해 별도 쿼리).
     *
     * @param userId 회원 id
     * @return 내 리뷰 목록(place·tags 로딩됨, createdAt DESC)
     */
    @Query("""
            SELECT DISTINCT r FROM Review r
            JOIN FETCH r.place
            LEFT JOIN FETCH r.tags
            WHERE r.user.id = :userId
            ORDER BY r.createdAt DESC, r.id DESC
            """)
    List<Review> findAllByUserWithPlaceAndTags(@Param("userId") Long userId);

    /**
     * 기능: 사용자의 혼밥 인증 리뷰(솔로 체크인 연결) 건수 집계 — 같이 먹은(matched) 체크인 리뷰는 제외
     * 쿼리: SELECT COUNT(*) FROM reviews r JOIN check_ins c ON r.check_in_id = c.id WHERE r.user_id = :userId AND c.matched_at IS NULL
     * Request: userId — 사용자 ID / Response: long — 혼밥 인증 리뷰 건수
     *
     * <p>[의도] '내 혼밥 기록' 요약 "일기 N"·더보기 카드 일기 수용. 인증(혼밥 뱃지)은 혼자 먹은(matchedAt IS NULL) 체크인만
     * 대상이므로({@link com.honjeong.review.service.ReviewService#resolveCheckIn}) 카운트도 솔로 기준으로 일치시킨다.
     *
     * @param userId 회원 id
     * @return 혼밥 인증 리뷰 건수(솔로 체크인 연결)
     */
    @Query("SELECT COUNT(r) FROM Review r WHERE r.user.id = :userId "
            + "AND r.checkIn IS NOT NULL AND r.checkIn.matchedAt IS NULL")
    long countSoloAuthenticatedByUser(@Param("userId") Long userId);

    /**
     * 기능: 해당 체크인에 연결된 리뷰가 이미 있는지 확인
     * 쿼리: SELECT COUNT(*) > 0 FROM reviews WHERE check_in_id = :checkInId
     * Request: checkInId — 체크인 ID / Response: boolean — 존재 여부
     *
     * <p>[기존 주석] 해당 체크인에 이미 리뷰가 있는지. 한 방문(체크인)=한 리뷰 제약(부분 유니크)의 사전 검사용.
     *
     * @param checkInId 체크인 id
     * @return 그 체크인에 연결된 리뷰 존재 여부
     */
    boolean existsByCheckIn_Id(Long checkInId);

    /**
     * 기능: 지정 사용자들의 이 식당 리뷰를 최신(visitedAt DESC)순으로 — 메이트 탭 "그 사람의 이 식당 평가" 배치 조회(N+1 방지)
     * Request: placeId, userIds / Response: 최신순 리뷰 목록(서비스에서 user별 첫 건만 사용)
     */
    List<Review> findByPlace_IdAndUser_IdInOrderByVisitedAtDesc(Long placeId, Collection<Long> userIds);
}
