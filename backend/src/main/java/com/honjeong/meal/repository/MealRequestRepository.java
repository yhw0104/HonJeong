package com.honjeong.meal.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.honjeong.meal.domain.MealRequest;
import com.honjeong.meal.domain.MealRequestStatus;

/**
 * 같이먹기 신청 저장소. 중복 방지는 DB 유니크(uq_meal_request_from_target)가 강제하고, 여기서는 조회 쿼리를 제공한다.
 * 목록은 fromUser를 fetch join해 닉네임 N+1을 막고, place는 프록시 getId만 쓰므로 fetch하지 않는다.
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
     * 내가 수신자인(toCheckIn.user = me) 신청 목록. status가 null이면 전체. fromUser는 fetch, toCheckIn·place는 프록시(id 접근만 안전). createdAt 내림차순.
     *
     * @param userId 수신자(나) id
     * @param status 상태 필터(null이면 전체)
     * @return 받은 신청 목록
     */
    @Query("""
            SELECT mr FROM MealRequest mr
            JOIN mr.toCheckIn ci
            JOIN FETCH mr.fromUser
            WHERE ci.user.id = :userId
              AND (:status IS NULL OR mr.status = :status)
            ORDER BY mr.createdAt DESC
            """)
    List<MealRequest> findReceived(@Param("userId") Long userId, @Param("status") MealRequestStatus status);

    /**
     * 내가 신청자인(fromUser = me) 신청 목록. status가 null이면 전체. fromUser는 fetch, toCheckIn·place는 프록시(id 접근만 안전). createdAt 내림차순.
     *
     * @param userId 신청자(나) id
     * @param status 상태 필터(null이면 전체)
     * @return 보낸 신청 목록
     */
    @Query("""
            SELECT mr FROM MealRequest mr
            JOIN FETCH mr.fromUser
            WHERE mr.fromUser.id = :userId
              AND (:status IS NULL OR mr.status = :status)
            ORDER BY mr.createdAt DESC
            """)
    List<MealRequest> findSent(@Param("userId") Long userId, @Param("status") MealRequestStatus status);
}
