package com.honjeong.mate.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.honjeong.mate.domain.MateRequest;
import com.honjeong.mate.domain.MateRequestStatus;

/**
 * 1. 기능: 메이트 신청 데이터 접근 (대상 테이블: mate_requests)
 */
public interface MateRequestRepository extends JpaRepository<MateRequest, Long> {

    /**
     * 기능: 신청 단건을 발신·수신 사용자와 함께 조회 (JOIN FETCH — 권한 검사·DTO 변환 시 N+1 방지)
     * 쿼리: SELECT mr.*, fu.*, tu.* FROM mate_requests mr JOIN users fu ON fu.id = mr.from_user_id JOIN users tu ON tu.id = mr.to_user_id WHERE mr.id = :id
     * Request: id — 신청 ID / Response: Optional<MateRequest> — 신청(없으면 empty)
     */
    @Query("""
            SELECT mr FROM MateRequest mr
            JOIN FETCH mr.fromUser
            JOIN FETCH mr.toUser
            WHERE mr.id = :id
            """)
    Optional<MateRequest> findWithUsersById(@Param("id") Long id);

    /**
     * 기능: 내가 받은 메이트 신청 목록을 최신순 조회 (상태 필터 선택, 차단 상대의 신청 제외, 발신·수신 사용자 JOIN FETCH)
     * 쿼리: SELECT mr.*, fu.*, tu.* FROM mate_requests mr JOIN users fu ON fu.id = mr.from_user_id JOIN users tu ON tu.id = mr.to_user_id
     *       WHERE mr.to_user_id = :userId AND (:status IS NULL OR mr.status = :status) AND mr.from_user_id NOT IN (:excludedUserIds) ORDER BY mr.created_at DESC
     * Request: userId — 수신자 ID, status — 상태 필터(null이면 전체), excludedUserIds — 제외할 사용자 ID 목록 / Response: List<MateRequest> — 받은 신청 목록
     * <p>[기존 주석] 차단 관계(양방향) 상대(fromUser=신청자)의 신청은 상호 은닉을 위해 제외한다(FR-108). excludedUserIds는 항상 non-empty.
     */
    @Query("""
            SELECT mr FROM MateRequest mr
            JOIN FETCH mr.fromUser
            JOIN FETCH mr.toUser
            WHERE mr.toUser.id = :userId
              AND (:status IS NULL OR mr.status = :status)
              AND mr.fromUser.id NOT IN :excludedUserIds
            ORDER BY mr.createdAt DESC
            """)
    List<MateRequest> findReceived(@Param("userId") Long userId, @Param("status") MateRequestStatus status,
            @Param("excludedUserIds") List<Long> excludedUserIds);

    /**
     * 기능: 내가 보낸 메이트 신청 목록을 최신순 조회 (상태 필터 선택, 차단 상대에게 보낸 신청 제외, 발신·수신 사용자 JOIN FETCH)
     * 쿼리: SELECT mr.*, fu.*, tu.* FROM mate_requests mr JOIN users fu ON fu.id = mr.from_user_id JOIN users tu ON tu.id = mr.to_user_id
     *       WHERE mr.from_user_id = :userId AND (:status IS NULL OR mr.status = :status) AND mr.to_user_id NOT IN (:excludedUserIds) ORDER BY mr.created_at DESC
     * Request: userId — 발신자 ID, status — 상태 필터(null이면 전체), excludedUserIds — 제외할 사용자 ID 목록 / Response: List<MateRequest> — 보낸 신청 목록
     * <p>[기존 주석] 차단 관계(양방향) 상대(toUser=수신자)로 보낸 신청은 상호 은닉을 위해 제외한다(FR-108). excludedUserIds는 항상 non-empty.
     */
    @Query("""
            SELECT mr FROM MateRequest mr
            JOIN FETCH mr.fromUser
            JOIN FETCH mr.toUser
            WHERE mr.fromUser.id = :userId
              AND (:status IS NULL OR mr.status = :status)
              AND mr.toUser.id NOT IN :excludedUserIds
            ORDER BY mr.createdAt DESC
            """)
    List<MateRequest> findSent(@Param("userId") Long userId, @Param("status") MateRequestStatus status,
            @Param("excludedUserIds") List<Long> excludedUserIds);

    // 관계상태(PENDING_SENT/PENDING_RECEIVED) 판정용: 두 유저 사이의 진행 중 신청.
    /**
     * 기능: 두 사용자 사이 특정 방향·특정 상태의 신청 단건 조회 (관계상태 판정·역방향 PENDING 정리용)
     * 쿼리: SELECT * FROM mate_requests WHERE from_user_id = :fromUserId AND to_user_id = :toUserId AND status = :status
     * Request: fromUserId — 발신자 ID, toUserId — 수신자 ID, status — 신청 상태 / Response: Optional<MateRequest> — 신청(없으면 empty)
     */
    Optional<MateRequest> findByFromUser_IdAndToUser_IdAndStatus(Long fromUserId, Long toUserId, MateRequestStatus status);

    /**
     * 기능: 두 사용자 사이 from→to 방향의 PENDING 신청을 지정 상태로 일괄 종결 (차단 시 자동 정리용)
     * 쿼리: UPDATE mate_requests SET status = :status, responded_at = :now WHERE from_user_id = :fromId AND to_user_id = :toId AND status = 'PENDING'
     * Request: fromId — 발신자 ID, toId — 수신자 ID, status — 종결 상태(CANCELED/DECLINED), now — 응답 시각 / Response: int — 갱신된 행 수
     * <p>[기존 주석] 두 유저 사이 방향별 PENDING 신청 일괄 종결(차단 자동 정리용). from→to 방향만 갱신한다.
     */
    @Modifying
    @Query("""
            UPDATE MateRequest mr
            SET mr.status = :status, mr.respondedAt = :now
            WHERE mr.fromUser.id = :fromId AND mr.toUser.id = :toId
              AND mr.status = com.honjeong.mate.domain.MateRequestStatus.PENDING
            """)
    int resolvePendingBetween(@Param("fromId") Long fromId, @Param("toId") Long toId,
            @Param("status") MateRequestStatus status, @Param("now") LocalDateTime now);
}
