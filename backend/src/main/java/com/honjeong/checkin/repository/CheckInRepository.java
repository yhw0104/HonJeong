package com.honjeong.checkin.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.honjeong.checkin.domain.CheckIn;
import com.honjeong.checkin.domain.CheckInStatus;

/**
 * 체크인 저장소. 단일 활성 제약은 DB 부분 유니크 인덱스가 강제하고, 여기서는 조회·집계 쿼리를 제공한다.
 */
public interface CheckInRepository extends JpaRepository<CheckIn, Long> {

    /**
     * 사용자의 특정 상태 체크인 1건. ACTIVE는 부분 유니크 인덱스로 최대 1개라 Optional이다.
     *
     * @param userId 회원 id
     * @param status 조회할 상태
     * @return 해당 체크인(없으면 빈 Optional)
     */
    Optional<CheckIn> findByUser_IdAndStatus(Long userId, CheckInStatus status);

    /**
     * 해당 상태의 전체 체크인 수(통계 activeCount용).
     *
     * @param status 셀 상태
     * @return 건수
     */
    long countByStatus(CheckInStatus status);

    /**
     * 기준 시각 이후 시작된 체크인의 distinct 사용자 수(오늘 혼밥 "N명").
     *
     * @param start 집계 시작 경계(KST 자정)
     * @return 중복 제거된 사용자 수
     */
    @Query("SELECT COUNT(DISTINCT c.user.id) FROM CheckIn c WHERE c.startedAt >= :start")
    long countDistinctUsersStartedSince(@Param("start") LocalDateTime start);
}
