package com.honjeong.meal.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.honjeong.meal.domain.MealRequest;
import com.honjeong.meal.domain.MealRequestStatus;

/**
 * 1. 기능: 같이먹기 신청 데이터 접근 — 권한검사용 단건 조회, 받은/보낸 목록, 일괄 거절(수락 정리·차단 정리), 함께먹음 집계 (대상 테이블: meal_requests)
 *
 * <p>[기존 주석] 같이먹기 신청 저장소. 중복 방지는 DB 유니크(uq_meal_request_from_target)가 강제하고, 여기서는 조회 쿼리를 제공한다.
 * 목록은 fromUser·toCheckIn.user·place를 fetch join해 닉네임·장소명 N+1을 막는다.
 */
public interface MealRequestRepository extends JpaRepository<MealRequest, Long> {

    /**
     * 기능: 신청 단건을 수신자(대상 체크인 소유자)까지 즉시 로딩해 조회(수락/거절 권한 검사용)
     * 쿼리: SELECT mr.*, ci.*, u.* FROM meal_requests mr JOIN check_ins ci ON mr.to_check_in_id = ci.id JOIN users u ON ci.user_id = u.id WHERE mr.id = :id (JOIN FETCH — 수신자 접근 시 추가 쿼리 방지)
     * Request: id — 신청 ID / Response: Optional&lt;MealRequest&gt; — 수신자 로딩된 신청, 없으면 빈 Optional
     *
     * <p>[기존 주석] 응답(수락/거절) 권한 검사용 — 수신자(toCheckIn.user)까지 fetch해 단건 조회한다.
     *
     * @param id 신청 id
     * @return 신청(수신자 로딩됨) 또는 빈 Optional
     */
    @Query("""
            SELECT mr FROM MealRequest mr
            JOIN FETCH mr.toCheckIn ci
            JOIN FETCH ci.user
            WHERE mr.id = :id
            """)
    Optional<MealRequest> findWithReceiverById(@Param("id") Long id);

    /**
     * 기능: 내가 수신자인 받은 신청 목록 조회(차단 상대 제외, 최신순)
     * 쿼리: SELECT mr.* FROM meal_requests mr JOIN check_ins ci ON mr.to_check_in_id = ci.id JOIN users 수신자 ON ci.user_id JOIN users 신청자 ON mr.from_user_id JOIN places ON mr.place_id
     *       WHERE ci.user_id = :userId AND (:status IS NULL OR mr.status = :status) AND mr.from_user_id NOT IN (:excludedUserIds) ORDER BY mr.created_at DESC
     *       (신청자·수신자·장소 전부 JOIN FETCH — 닉네임·장소명 N+1 방지)
     * Request: userId — 수신자(나) ID, status — 상태 필터(null=전체), excludedUserIds — 제외할 신청자 ID 목록 / Response: List&lt;MealRequest&gt; — 받은 신청 목록
     *
     * <p>[기존 주석] 내가 수신자인(toCheckIn.user = me) 신청 목록. status가 null이면 전체. fromUser·toCheckIn.user·place를 fetch join. createdAt 내림차순.
     * 차단 관계(양방향) 상대(fromUser=신청자)의 신청은 상호 은닉을 위해 제외한다(FR-108).
     *
     * @param userId          수신자(나) id
     * @param status          상태 필터(null이면 전체)
     * @param excludedUserIds 제외할 신청자 id 목록(차단 상호 은닉용, 항상 non-empty)
     * @return 받은 신청 목록(제외 대상 제외)
     */
    @Query("""
            SELECT mr FROM MealRequest mr
            JOIN FETCH mr.toCheckIn ci
            JOIN FETCH ci.user
            JOIN FETCH mr.fromUser
            JOIN FETCH mr.place
            WHERE ci.user.id = :userId
              AND (:status IS NULL OR mr.status = :status)
              AND mr.fromUser.id NOT IN :excludedUserIds
            ORDER BY mr.createdAt DESC
            """)
    List<MealRequest> findReceived(@Param("userId") Long userId, @Param("status") MealRequestStatus status,
            @Param("excludedUserIds") List<Long> excludedUserIds);

    /**
     * 기능: 내가 신청자인 보낸 신청 목록 조회(차단 상대 제외, 최신순)
     * 쿼리: SELECT mr.* FROM meal_requests mr JOIN users 신청자 ON mr.from_user_id JOIN check_ins ci ON mr.to_check_in_id = ci.id JOIN users 수신자 ON ci.user_id JOIN places ON mr.place_id
     *       WHERE mr.from_user_id = :userId AND (:status IS NULL OR mr.status = :status) AND ci.user_id NOT IN (:excludedUserIds) ORDER BY mr.created_at DESC
     *       (신청자·수신자·장소 전부 JOIN FETCH — 닉네임·장소명 N+1 방지)
     * Request: userId — 신청자(나) ID, status — 상태 필터(null=전체), excludedUserIds — 제외할 수신자 ID 목록 / Response: List&lt;MealRequest&gt; — 보낸 신청 목록
     *
     * <p>[기존 주석] 내가 신청자인(fromUser = me) 신청 목록. status가 null이면 전체. fromUser·toCheckIn.user·place를 fetch join. createdAt 내림차순.
     * 차단 관계(양방향) 상대(toCheckIn.user=수신자)로 보낸 신청은 상호 은닉을 위해 제외한다(FR-108).
     *
     * @param userId          신청자(나) id
     * @param status          상태 필터(null이면 전체)
     * @param excludedUserIds 제외할 수신자 id 목록(차단 상호 은닉용, 항상 non-empty)
     * @return 보낸 신청 목록(제외 대상 제외)
     */
    @Query("""
            SELECT mr FROM MealRequest mr
            JOIN FETCH mr.fromUser
            JOIN FETCH mr.toCheckIn ci
            JOIN FETCH ci.user
            JOIN FETCH mr.place
            WHERE mr.fromUser.id = :userId
              AND (:status IS NULL OR mr.status = :status)
              AND ci.user.id NOT IN :excludedUserIds
            ORDER BY mr.createdAt DESC
            """)
    List<MealRequest> findSent(@Param("userId") Long userId, @Param("status") MealRequestStatus status,
            @Param("excludedUserIds") List<Long> excludedUserIds);

    /**
     * 기능: 같은 대상 체크인으로 온 나머지 PENDING 신청 일괄 거절(하나 수락 시 자동 정리)
     * 쿼리: UPDATE meal_requests SET status = 'DECLINED', responded_at = :now WHERE to_check_in_id = :toCheckInId AND id &lt;&gt; :exceptId AND status = 'PENDING'
     * Request: toCheckInId — 대상 체크인 ID, exceptId — 방금 수락한 신청 ID(제외), now — 응답 시각 / Response: int — 거절 처리된 행 수
     *
     * <p>[기존 주석] 같은 대상 체크인으로 온 나머지 PENDING 신청을 일괄 DECLINED 처리한다(수락 시 정리). exceptId=방금 수락한 신청.
     */
    @Modifying
    @Query("""
            UPDATE MealRequest mr
            SET mr.status = com.honjeong.meal.domain.MealRequestStatus.DECLINED, mr.respondedAt = :now
            WHERE mr.toCheckIn.id = :toCheckInId AND mr.id <> :exceptId
              AND mr.status = com.honjeong.meal.domain.MealRequestStatus.PENDING
            """)
    int declineOtherPending(@Param("toCheckInId") Long toCheckInId,
            @Param("exceptId") Long exceptId, @Param("now") LocalDateTime now);

    /**
     * 기능: 두 유저 사이(방향 무관) PENDING 신청 일괄 거절 — BlockService가 차단 시 자동 정리용으로 호출
     * 쿼리: UPDATE meal_requests SET status = 'DECLINED', responded_at = :now WHERE status = 'PENDING'
     *       AND ((from_user_id = :a AND to_check_in_id IN (SELECT id FROM check_ins WHERE user_id = :b))
     *         OR (from_user_id = :b AND to_check_in_id IN (SELECT id FROM check_ins WHERE user_id = :a)))
     * Request: a — 한쪽 사용자 ID, b — 다른쪽 사용자 ID, now — 응답 시각 / Response: int — 거절 처리된 행 수
     *
     * <p>[기존 주석] 두 유저 사이(방향 무관) PENDING 같이먹기 신청 일괄 DECLINED(차단 자동 정리용).
     */
    @Modifying
    @Query("""
            UPDATE MealRequest mr
            SET mr.status = com.honjeong.meal.domain.MealRequestStatus.DECLINED, mr.respondedAt = :now
            WHERE mr.status = com.honjeong.meal.domain.MealRequestStatus.PENDING
              AND ((mr.fromUser.id = :a AND mr.toCheckIn.id IN
                        (SELECT c.id FROM CheckIn c WHERE c.user.id = :b))
                OR (mr.fromUser.id = :b AND mr.toCheckIn.id IN
                        (SELECT c.id FROM CheckIn c WHERE c.user.id = :a)))
            """)
    int declinePendingBetween(@Param("a") Long a, @Param("b") Long b, @Param("now") LocalDateTime now);
}
