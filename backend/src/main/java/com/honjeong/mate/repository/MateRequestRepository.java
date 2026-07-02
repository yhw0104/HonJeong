package com.honjeong.mate.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.honjeong.mate.domain.MateRequest;
import com.honjeong.mate.domain.MateRequestStatus;

public interface MateRequestRepository extends JpaRepository<MateRequest, Long> {

    @Query("""
            SELECT mr FROM MateRequest mr
            JOIN FETCH mr.fromUser
            JOIN FETCH mr.toUser
            WHERE mr.id = :id
            """)
    Optional<MateRequest> findWithUsersById(@Param("id") Long id);

    @Query("""
            SELECT mr FROM MateRequest mr
            JOIN FETCH mr.fromUser
            JOIN FETCH mr.toUser
            WHERE mr.toUser.id = :userId
              AND (:status IS NULL OR mr.status = :status)
            ORDER BY mr.createdAt DESC
            """)
    List<MateRequest> findReceived(@Param("userId") Long userId, @Param("status") MateRequestStatus status);

    @Query("""
            SELECT mr FROM MateRequest mr
            JOIN FETCH mr.fromUser
            JOIN FETCH mr.toUser
            WHERE mr.fromUser.id = :userId
              AND (:status IS NULL OR mr.status = :status)
            ORDER BY mr.createdAt DESC
            """)
    List<MateRequest> findSent(@Param("userId") Long userId, @Param("status") MateRequestStatus status);

    // 관계상태(PENDING_SENT/PENDING_RECEIVED) 판정용: 두 유저 사이의 진행 중 신청.
    Optional<MateRequest> findByFromUser_IdAndToUser_IdAndStatus(Long fromUserId, Long toUserId, MateRequestStatus status);
}
