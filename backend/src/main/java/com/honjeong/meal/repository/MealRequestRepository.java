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
 * 같이먹기 신청 저장소. 중복 방지는 DB 유니크(uq_meal_request_from_target)가 강제하고, 여기서는 조회 쿼리를 제공한다.
 * 목록은 fromUser·toCheckIn.user·place를 fetch join해 닉네임·장소명 N+1을 막는다.
 */
public interface MealRequestRepository extends JpaRepository<MealRequest, Long> {

    /**
     * 응답(수락/거절) 권한 검사용 — 수신자(toCheckIn.user)까지 fetch해 단건 조회한다.
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
     * 내가 수신자인(toCheckIn.user = me) 신청 목록. status가 null이면 전체. fromUser·toCheckIn.user·place를 fetch join. createdAt 내림차순.
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
     * 내가 신청자인(fromUser = me) 신청 목록. status가 null이면 전체. fromUser·toCheckIn.user·place를 fetch join. createdAt 내림차순.
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

    /** 같은 대상 체크인으로 온 나머지 PENDING 신청을 일괄 DECLINED 처리한다(수락 시 정리). exceptId=방금 수락한 신청. */
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
     * 두 사용자가 함께 먹은 횟수 = 수락(ACCEPTED)된 같이먹기 신청 수. 방향 무관 —
     * a가 신청하고 b가 수신했거나 그 반대 모두 센다(상대방 프로필 "함께 먹음" 통계).
     *
     * @param a 한쪽 사용자 id(보통 뷰어)
     * @param b 다른쪽 사용자 id(보통 대상)
     * @return 두 사람 사이 수락된 같이먹기 건수
     */
    @Query("""
            SELECT COUNT(mr) FROM MealRequest mr
            WHERE mr.status = com.honjeong.meal.domain.MealRequestStatus.ACCEPTED
              AND ((mr.fromUser.id = :a AND mr.toCheckIn.user.id = :b)
                OR (mr.fromUser.id = :b AND mr.toCheckIn.user.id = :a))
            """)
    long countAcceptedBetween(@Param("a") Long a, @Param("b") Long b);

    /**
     * viewer가 참여한 수락된 같이먹기의 (신청자, 수신자) 사용자 id 쌍 목록. 메이트 목록의
     * 상대별 "함께 먹음"을 한 번에 집계하기 위한 배치 조회다(메이트마다 count 날리는 N+1 방지).
     * 상대 id는 호출 측에서 {@code fromId==viewer ? toId : fromId}로 뽑아 합산한다.
     *
     * @param viewerId 기준 사용자(나) id
     * @return viewer가 신청자이거나 수신자인 ACCEPTED 신청들의 (fromId, toId) 쌍
     */
    @Query("""
            SELECT mr.fromUser.id AS fromId, mr.toCheckIn.user.id AS toId
            FROM MealRequest mr
            WHERE mr.status = com.honjeong.meal.domain.MealRequestStatus.ACCEPTED
              AND (mr.fromUser.id = :viewerId OR mr.toCheckIn.user.id = :viewerId)
            """)
    List<MealPairRow> findAcceptedPairsForUser(@Param("viewerId") Long viewerId);

    /** 수락된 같이먹기 한 건의 신청자·수신자 사용자 id 쌍(함께먹음 배치 집계용). */
    interface MealPairRow {
        Long getFromId();

        Long getToId();
    }

    /** 두 유저 사이(방향 무관) PENDING 같이먹기 신청 일괄 DECLINED(차단 자동 정리용). */
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
