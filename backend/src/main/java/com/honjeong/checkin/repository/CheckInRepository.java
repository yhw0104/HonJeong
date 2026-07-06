package com.honjeong.checkin.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.honjeong.checkin.domain.CheckIn;
import com.honjeong.checkin.domain.CheckInStatus;
import com.honjeong.checkin.dto.MapMarkerResponse;
import com.honjeong.checkin.dto.PlaceActiveCount;

/**
 * 1. 기능: 혼밥 체크인 데이터 접근 — 단건 조회·통계 집계·지도 마커·이력·일괄 만료 쿼리 (대상 테이블: check_ins)
 *
 * <p>[기존 주석] 체크인 저장소. 단일 활성 제약은 DB 부분 유니크 인덱스가 강제하고, 여기서는 조회·집계 쿼리를 제공한다.
 */
public interface CheckInRepository extends JpaRepository<CheckIn, Long> {

    /**
     * 기능: 사용자의 특정 상태 체크인 1건 조회
     * 쿼리: SELECT * FROM check_ins WHERE user_id = :userId AND status = :status
     * Request: userId — 회원 ID, status — 조회할 상태 / Response: Optional&lt;CheckIn&gt; — 해당 체크인(없으면 빈 Optional)
     *
     * <p>[기존 주석] 사용자의 특정 상태 체크인 1건. ACTIVE는 부분 유니크 인덱스로 최대 1개라 Optional이다.
     *
     * @param userId 회원 id
     * @param status 조회할 상태
     * @return 해당 체크인(없으면 빈 Optional)
     */
    Optional<CheckIn> findByUser_IdAndStatus(Long userId, CheckInStatus status);

    /**
     * 기능: 사용자의 현재 활동 체크인(ACTIVE 또는 TOGETHER) 1건 조회
     * 쿼리: SELECT * FROM check_ins WHERE user_id = :userId AND status IN (:statuses)
     * Request: userId — 회원 ID, statuses — 조회할 상태 집합 / Response: Optional&lt;CheckIn&gt; — 해당 체크인(없으면 빈 Optional)
     *
     * <p>[기존 주석] 사용자의 현재 활동 체크인(ACTIVE 또는 TOGETHER) 1건. 확장 유니크 인덱스(uq_check_ins_current_user)로 최대 1개다.
     *
     * @param userId   회원 id
     * @param statuses 조회할 상태 집합(보통 ACTIVE, TOGETHER)
     * @return 해당 체크인(없으면 빈 Optional)
     */
    Optional<CheckIn> findByUser_IdAndStatusIn(Long userId, Collection<CheckInStatus> statuses);

    /**
     * 기능: 특정 상태를 제외한 사용자의 체크인 수 집계
     * 쿼리: SELECT COUNT(*) FROM check_ins WHERE user_id = :userId AND status <> :status
     * Request: userId — 회원 ID, status — 제외할 상태 / Response: long — 건수
     *
     * <p>[기존 주석] 특정 상태를 제외한 사용자 체크인 수(총 체크인에서 CANCELLED 제외용).
     *
     * @param userId 회원 id
     * @param status 제외할 상태
     * @return 건수
     */
    long countByUser_IdAndStatusNot(Long userId, CheckInStatus status);

    /**
     * 기능: 해당 상태의 전체 체크인 수 집계
     * 쿼리: SELECT COUNT(*) FROM check_ins WHERE status = :status
     * Request: status — 셀 상태 / Response: long — 건수
     *
     * <p>[기존 주석] 해당 상태의 전체 체크인 수(통계 activeCount용).
     *
     * @param status 셀 상태
     * @return 건수
     */
    long countByStatus(CheckInStatus status);

    /**
     * 기능: 기준 시각 이후 시작된 체크인의 distinct 사용자 수 집계(오늘 혼밥 "N명")
     * 쿼리: SELECT COUNT(DISTINCT user_id) FROM check_ins WHERE started_at >= :start AND status <> 'CANCELLED'
     * Request: start — 집계 시작 경계(KST 자정) / Response: long — 중복 제거된 사용자 수
     *
     * <p>[기존 주석] 기준 시각 이후 시작된 체크인의 distinct 사용자 수(오늘 혼밥 "N명").
     *
     * @param start 집계 시작 경계(KST 자정)
     * @return 중복 제거된 사용자 수
     */
    @Query("SELECT COUNT(DISTINCT c.user.id) FROM CheckIn c "
            + "WHERE c.startedAt >= :start AND c.status <> com.honjeong.checkin.domain.CheckInStatus.CANCELLED")
    long countDistinctUsersStartedSince(@Param("start") LocalDateTime start);

    /**
     * 기능: 위경도 박스 안의 식당별 현재 ACTIVE 혼밥러 수를 마커 DTO로 집계
     * 쿼리: SELECT p.id, p.name, p.latitude, p.longitude, COUNT(c.id) FROM check_ins c JOIN places p ON c.place_id = p.id
     *       WHERE c.status = 'ACTIVE' AND p.latitude BETWEEN :latMin AND :latMax AND p.longitude BETWEEN :lngMin AND :lngMax
     *       GROUP BY p.id, p.name, p.latitude, p.longitude (INNER JOIN이라 ACTIVE가 있는 식당만 반환)
     * Request: latMin·latMax — 위도 하한·상한, lngMin·lngMax — 경도 하한·상한 / Response: List&lt;MapMarkerResponse&gt; — 박스 내 식당별 마커
     *
     * <p>[기존 주석] 위경도 박스 안의 식당별 현재 ACTIVE 혼밥러 수. ACTIVE가 있는 식당만(INNER JOIN) 반환한다.
     * 원형 반경 보정·거리정렬은 서비스가 Haversine로 수행한다.
     *
     * @param latMin 위도 하한
     * @param latMax 위도 상한
     * @param lngMin 경도 하한
     * @param lngMax 경도 상한
     * @return 박스 내 식당별 마커(활성 수 포함)
     */
    @Query("""
            SELECT new com.honjeong.checkin.dto.MapMarkerResponse(p.id, p.name, p.latitude, p.longitude, COUNT(c.id))
            FROM CheckIn c JOIN c.place p
            WHERE c.status = com.honjeong.checkin.domain.CheckInStatus.ACTIVE
              AND p.latitude BETWEEN :latMin AND :latMax
              AND p.longitude BETWEEN :lngMin AND :lngMax
            GROUP BY p.id, p.name, p.latitude, p.longitude
            """)
    List<MapMarkerResponse> countActiveByPlaceWithinBounds(
            @Param("latMin") double latMin, @Param("latMax") double latMax,
            @Param("lngMin") double lngMin, @Param("lngMax") double lngMax);

    /**
     * 기능: 식당의 현재 ACTIVE 체크인을 사용자(닉네임)와 함께 시작시각 오름차순으로 조회
     * 쿼리: SELECT c.*, u.* FROM check_ins c JOIN users u ON c.user_id = u.id
     *       WHERE c.place_id = :placeId AND c.status = 'ACTIVE' AND c.user_id NOT IN (:excludedUserIds)
     *       ORDER BY c.started_at (user를 fetch join해 N+1 방지)
     * Request: placeId — 식당 ID, excludedUserIds — 제외할 사용자 ID 목록 / Response: List&lt;CheckIn&gt; — ACTIVE 체크인 목록(user 로딩됨)
     *
     * <p>[기존 주석] 식당의 현재 ACTIVE 체크인을 startedAt 오름차순으로 조회한다. 닉네임을 위해 user를 fetch join한다(N+1 방지).
     * 차단 관계(양방향) 유저는 상호 은닉하기 위해 {@code excludedUserIds}로 걸러낸다(FR-108).
     *
     * @param placeId         식당 id
     * @param excludedUserIds 제외할 사용자 id 목록(차단 상호 은닉용, 항상 non-empty — 빈 IN 방지는 호출 측 책임)
     * @return ACTIVE 체크인 목록(user 로딩됨, 제외 대상 제외)
     */
    @Query("""
            SELECT c FROM CheckIn c JOIN FETCH c.user
            WHERE c.place.id = :placeId AND c.status = com.honjeong.checkin.domain.CheckInStatus.ACTIVE
              AND c.user.id NOT IN :excludedUserIds
            ORDER BY c.startedAt
            """)
    List<CheckIn> findActiveWithUserByPlace(@Param("placeId") Long placeId,
            @Param("excludedUserIds") List<Long> excludedUserIds);

    /**
     * 기능: 주어진 장소 ID 목록의 현재 ACTIVE 체크인 수를 장소별로 배치 집계
     * 쿼리: SELECT place_id, COUNT(id) FROM check_ins WHERE place_id IN (:placeIds) AND status = 'ACTIVE' GROUP BY place_id
     * Request: placeIds — 조회할 장소 PK 목록 / Response: List&lt;PlaceActiveCount&gt; — 장소별 ACTIVE 수(ACTIVE 있는 장소만)
     *
     * <p>[기존 주석] 주어진 장소 ID 목록에 대해 현재 ACTIVE 체크인 수를 장소별로 집계한다.
     * ACTIVE가 없는 장소는 결과에 포함되지 않는다(카운트 0은 서비스에서 기본값으로 처리).
     *
     * <p><b>주의:</b> placeIds가 빈 리스트이면 JPQL {@code IN ()} 오류가 발생할 수 있으므로
     * 호출 전 반드시 빈 리스트 여부를 확인하고 단락 처리해야 한다.
     *
     * @param placeIds 조회할 장소 PK 목록
     * @return 장소별 ACTIVE 혼밥러 수(ACTIVE가 있는 장소만 포함)
     */
    @Query("""
            SELECT new com.honjeong.checkin.dto.PlaceActiveCount(c.place.id, COUNT(c.id))
            FROM CheckIn c
            WHERE c.place.id IN :placeIds AND c.status = com.honjeong.checkin.domain.CheckInStatus.ACTIVE
            GROUP BY c.place.id
            """)
    List<PlaceActiveCount> countActiveByPlaceIds(@Param("placeIds") List<Long> placeIds);

    /**
     * 기능: 리뷰 인증 자동연결용 — 해당 식당에 대한 사용자의 최근 솔로 체크인(ACTIVE 또는 since 이후 ENDED) 1건 조회
     * 쿼리: SELECT * FROM check_ins WHERE user_id = :userId AND place_id = :placeId AND matched_at IS NULL
     *       AND (status = 'ACTIVE' OR (status = 'ENDED' AND ended_at >= :since)) ORDER BY started_at DESC LIMIT 1
     * Request: userId — 회원 ID, placeId — 식당 ID, since — ENDED 최소 종료 시각 / Response: Optional&lt;CheckIn&gt; — 가장 최근 체크인
     *
     * <p>[기존 주석] place에 대한 user의 최근 체크인(ACTIVE 또는 since 이후 ENDED). 리뷰 인증 자동연결용.
     * 같이먹기로 매칭됐던(matchedAt not null) 체크인은 제외한다 — 혼밥 리뷰 자동연결은 솔로 체크인만 대상으로 한다.
     *
     * @param userId  회원 id
     * @param placeId 식당 id
     * @param since   ENDED 체크인의 최소 종료 시각(24h 창)
     * @return 가장 최근 체크인(없으면 빈 Optional)
     */
    @Query("""
            SELECT c FROM CheckIn c
            WHERE c.user.id = :userId AND c.place.id = :placeId AND c.matchedAt IS NULL
              AND (c.status = com.honjeong.checkin.domain.CheckInStatus.ACTIVE
                   OR (c.status = com.honjeong.checkin.domain.CheckInStatus.ENDED AND c.endedAt >= :since))
            ORDER BY c.startedAt DESC
            LIMIT 1
            """)
    Optional<CheckIn> findRecentForReview(@Param("userId") Long userId,
            @Param("placeId") Long placeId, @Param("since") LocalDateTime since);

    /**
     * 기능: 같은 매칭(meal_request_id)에 묶인 양쪽 TOGETHER 체크인 조회(파트너 동시 종료·파트너 닉네임 조회용)
     * 쿼리: SELECT c.*, u.* FROM check_ins c JOIN users u ON c.user_id = u.id
     *       WHERE c.meal_request_id = :mealRequestId AND c.status = 'TOGETHER' (user fetch join)
     * Request: mealRequestId — 매칭 신청 ID / Response: List&lt;CheckIn&gt; — 해당 매칭의 TOGETHER 체크인들(user 로딩됨)
     *
     * <p>[기존 주석] 같은 매칭(meal_request_id)에 묶인 TOGETHER 체크인들(양쪽). 파트너 동시 종료·파트너 조회용. user fetch join.
     *
     * @param mealRequestId 매칭 신청 id
     * @return 해당 매칭의 TOGETHER 체크인들(user 로딩됨)
     */
    @Query("""
            SELECT c FROM CheckIn c JOIN FETCH c.user
            WHERE c.mealRequestId = :mealRequestId AND c.status = com.honjeong.checkin.domain.CheckInStatus.TOGETHER
            """)
    List<CheckIn> findTogetherByMealRequestId(@Param("mealRequestId") Long mealRequestId);

    /**
     * 기능: matched_at이 기준 이전인 TOGETHER 체크인을 일괄 ENDED 처리(같이먹기 TTL 만료)
     * 쿼리: UPDATE check_ins SET status = 'ENDED', ended_at = :now WHERE status = 'TOGETHER' AND matched_at < :threshold
     * Request: threshold — 만료 기준 시각, now — 종료 시각으로 기록할 현재 시각 / Response: int — 만료된 건수
     *
     * <p>[기존 주석] matched_at이 기준 이전인 TOGETHER를 일괄 ENDED 처리한다(같이먹기 TTL 만료).
     *
     * @param threshold 이 시각 이전 매칭된 TOGETHER가 만료 대상
     * @param now       종료 시각으로 기록할 현재 시각
     * @return 만료된 건수
     */
    @Modifying
    @Query("""
            UPDATE CheckIn c SET c.status = com.honjeong.checkin.domain.CheckInStatus.ENDED, c.endedAt = :now
            WHERE c.status = com.honjeong.checkin.domain.CheckInStatus.TOGETHER AND c.matchedAt < :threshold
            """)
    int endTogetherMatchedBefore(@Param("threshold") LocalDateTime threshold, @Param("now") LocalDateTime now);

    /**
     * 기능: started_at이 기준 이전인 ACTIVE 체크인을 일괄 ENDED 처리(TTL 자동 만료)
     * 쿼리: UPDATE check_ins SET status = 'ENDED', ended_at = :now WHERE status = 'ACTIVE' AND started_at < :threshold
     * Request: threshold — 만료 기준 시각, now — 종료 시각으로 기록할 현재 시각 / Response: int — 만료된 건수
     *
     * <p>[기존 주석] 기준 시각 이전 시작된 ACTIVE를 일괄 ENDED 처리하고 만료 건수를 반환한다(TTL 자동 만료).
     *
     * @param threshold 이 시각 이전 시작된 ACTIVE가 만료 대상
     * @param now       종료 시각으로 기록할 현재 시각
     * @return 만료된 건수
     */
    @Modifying
    @Query("""
            UPDATE CheckIn c SET c.status = com.honjeong.checkin.domain.CheckInStatus.ENDED, c.endedAt = :now
            WHERE c.status = com.honjeong.checkin.domain.CheckInStatus.ACTIVE AND c.startedAt < :threshold
            """)
    int endActiveStartedBefore(@Param("threshold") LocalDateTime threshold, @Param("now") LocalDateTime now);

    /**
     * 기능: 사용자의 전체 체크인 이력을 장소와 함께 최신순으로 조회(타임라인용)
     * 쿼리: SELECT c.*, p.* FROM check_ins c JOIN places p ON c.place_id = p.id
     *       WHERE c.user_id = :userId AND c.status <> 'CANCELLED' ORDER BY c.started_at DESC (place fetch join)
     * Request: userId — 회원 ID / Response: List&lt;CheckIn&gt; — 체크인 이력(place 로딩됨, 최신순)
     *
     * <p>[기존 주석] 사용자의 전체 체크인 이력을 place와 함께 startedAt 내림차순으로 조회한다(타임라인용).
     *
     * @param userId 회원 id
     * @return 체크인 이력(place fetch join 포함, 최신순)
     */
    @Query("SELECT c FROM CheckIn c JOIN FETCH c.place "
            + "WHERE c.user.id = :userId AND c.status <> com.honjeong.checkin.domain.CheckInStatus.CANCELLED "
            + "ORDER BY c.startedAt DESC")
    List<CheckIn> findHistoryWithPlaceByUser(@Param("userId") Long userId);

    /**
     * 기능: 사용자의 전체 체크인 수 집계
     * 쿼리: SELECT COUNT(*) FROM check_ins WHERE user_id = :userId
     * Request: userId — 회원 ID / Response: long — 총 체크인 건수
     *
     * <p>[기존 주석] 사용자의 전체 체크인 수.
     *
     * @param userId 회원 id
     * @return 총 체크인 건수
     */
    long countByUser_Id(Long userId);

    /**
     * 기능: 사용자가 방문한 식당 수 집계(중복 제거)
     * 쿼리: SELECT COUNT(DISTINCT place_id) FROM check_ins WHERE user_id = :userId AND status <> 'CANCELLED'
     * Request: userId — 회원 ID / Response: long — 고유 식당 수
     *
     * <p>[기존 주석] 사용자가 방문한 식당 수(중복 제거).
     *
     * @param userId 회원 id
     * @return 고유 식당 수
     */
    @Query("SELECT COUNT(DISTINCT c.place.id) FROM CheckIn c "
            + "WHERE c.user.id = :userId AND c.status <> com.honjeong.checkin.domain.CheckInStatus.CANCELLED")
    long countDistinctPlacesByUser(@Param("userId") Long userId);

    /**
     * 기능: 기준 시각 이후 사용자의 체크인 수 집계(이번 달 체크인 수용)
     * 쿼리: SELECT COUNT(*) FROM check_ins WHERE user_id = :userId AND started_at >= :monthStart AND status <> 'CANCELLED'
     * Request: userId — 회원 ID, monthStart — 집계 시작 경계 / Response: long — 해당 기간 체크인 건수
     *
     * <p>[기존 주석] 기준 시각 이후 사용자의 체크인 수(이번 달 체크인 수용).
     *
     * @param userId     회원 id
     * @param monthStart 집계 시작 경계
     * @return 해당 기간 체크인 건수
     */
    @Query("SELECT COUNT(c) FROM CheckIn c WHERE c.user.id = :userId AND c.startedAt >= :monthStart "
            + "AND c.status <> com.honjeong.checkin.domain.CheckInStatus.CANCELLED")
    long countByUserSince(@Param("userId") Long userId, @Param("monthStart") LocalDateTime monthStart);

    /**
     * 기능: 주어진 장소 목록 중 사용자가 체크인한 적 있는 장소 ID 조회(즐겨찾기 visited 판정용)
     * 쿼리: SELECT DISTINCT place_id FROM check_ins WHERE user_id = :userId AND place_id IN (:placeIds) AND status <> 'CANCELLED'
     * Request: userId — 회원 ID, placeIds — 판정할 장소 PK 목록 / Response: List&lt;Long&gt; — 체크인 이력이 있는 장소 ID 목록
     *
     * <p>[기존 주석] 주어진 장소 id 목록 중 사용자가 체크인한 적 있는 장소 id들을 반환한다(즐겨찾기 visited 판정용).
     *
     * @param userId   회원 id
     * @param placeIds 판정할 장소 pk 목록
     * @return 체크인 이력이 있는 장소 id 목록(중복 제거)
     */
    @Query("SELECT DISTINCT c.place.id FROM CheckIn c "
            + "WHERE c.user.id = :userId AND c.place.id IN :placeIds "
            + "AND c.status <> com.honjeong.checkin.domain.CheckInStatus.CANCELLED")
    List<Long> findVisitedPlaceIds(@Param("userId") Long userId, @Param("placeIds") List<Long> placeIds);

    /**
     * 기능: 주어진 사용자 목록의 현재 ACTIVE 체크인을 장소와 함께 배치 조회(메이트 온라인 상태 표시용)
     * 쿼리: SELECT c.*, p.* FROM check_ins c JOIN places p ON c.place_id = p.id
     *       WHERE c.user_id IN (:userIds) AND c.status = 'ACTIVE' (place fetch join으로 N+1 방지)
     * Request: userIds — 조회할 사용자 PK 목록 / Response: List&lt;CheckIn&gt; — 해당 사용자들의 ACTIVE 체크인(place 로딩됨)
     *
     * <p>[기존 주석] 주어진 사용자 id 목록의 현재 ACTIVE 체크인을 place와 함께 배치 조회한다(메이트 온라인 상태 N+1 방지).
     *
     * <p><b>주의:</b> userIds가 빈 리스트이면 JPQL {@code IN ()} 오류가 발생할 수 있으므로
     * 호출 전 반드시 빈 리스트 여부를 확인하고 단락 처리해야 한다.
     *
     * @param userIds 조회할 사용자 PK 목록
     * @return 해당 사용자들의 ACTIVE 체크인(place fetch join 포함)
     */
    @Query("""
            SELECT c FROM CheckIn c JOIN FETCH c.place
            WHERE c.user.id IN :userIds AND c.status = com.honjeong.checkin.domain.CheckInStatus.ACTIVE
            """)
    List<CheckIn> findActiveWithPlaceByUserIds(@Param("userIds") List<Long> userIds);

    /**
     * 기능: 주어진 사용자 목록의 체크인 수(혼밥 횟수)를 사용자별로 배치 집계(메이트 목록 checkInCount용)
     * 쿼리: SELECT user_id, COUNT(*) FROM check_ins WHERE user_id IN (:userIds) AND status <> 'CANCELLED' GROUP BY user_id
     * Request: userIds — 조회할 사용자 PK 목록 / Response: List&lt;CheckInCountRow&gt; — 사용자별 체크인 수 행(유효 체크인 있는 사용자만)
     *
     * <p>[기존 주석] 주어진 사용자 id 목록의 체크인 수(혼밥 횟수)를 사용자별로 배치 집계한다(메이트 목록 checkInCount N+1 방지).
     * 본인 프로필·메이트 상세와 같은 기준({@code countByUser_IdAndStatusNot(CANCELLED)})으로
     * CANCELLED(30분 미만 취소)는 제외한다 — 포함하면 화면마다 혼밥 횟수가 달라진다.
     * 유효 체크인이 0건인 사용자는 결과에 포함되지 않는다(호출 측에서 기본값 0으로 처리).
     *
     * <p><b>주의:</b> userIds가 빈 리스트이면 JPQL {@code IN ()} 오류가 발생할 수 있으므로
     * 호출 전 반드시 빈 리스트 여부를 확인하고 단락 처리해야 한다.
     *
     * @param userIds 조회할 사용자 PK 목록
     * @return 사용자별 체크인 수 행(유효 체크인이 있는 사용자만 포함)
     */
    @Query("""
            SELECT c.user.id AS userId, COUNT(c) AS cnt FROM CheckIn c
            WHERE c.user.id IN :userIds AND c.status <> com.honjeong.checkin.domain.CheckInStatus.CANCELLED
            GROUP BY c.user.id
            """)
    List<CheckInCountRow> countByUserIds(@Param("userIds") List<Long> userIds);

    /** 사용자별 체크인 수 한 행(사용자 id + 건수). */
    interface CheckInCountRow {
        /** 사용자 ID */
        Long getUserId();

        /** 유효 체크인 건수(CANCELLED 제외) */
        long getCnt();
    }
}
