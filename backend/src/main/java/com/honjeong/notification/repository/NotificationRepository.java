package com.honjeong.notification.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.honjeong.notification.domain.Notification;

/**
 * 1. 기능: 인앱 알림 데이터 접근 — 목록 조회·안읽음 개수·전체 읽음 처리 (대상 테이블: notifications)
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 기능: 내 알림 최근 30건 조회(보존기간 이후분·최신순)
     * 쿼리: SELECT n.*, actor.* FROM notifications n LEFT JOIN users actor ON n.actor_user_id = actor.id WHERE n.user_id = :userId AND n.created_at &gt; :after ORDER BY n.created_at DESC LIMIT 30
     * Request: userId — 조회 사용자 ID, after — 보존 기준 시각(이 시각 이후분만) / Response: List&lt;Notification&gt; — 알림 목록(actor 즉시 로드)
     *
     * <p>[기존 주석] 내 알림 최근 30건(보존기간 이후분). actor를 EntityGraph로 함께 로드해
     * DTO 변환 시 닉네임 접근 N+1을 막는다.
     */
    @EntityGraph(attributePaths = "actor")
    List<Notification> findTop30ByUser_IdAndCreatedAtAfterOrderByCreatedAtDesc(Long userId, LocalDateTime after);

    /**
     * 기능: 내 안읽은 알림 개수 조회
     * 쿼리: SELECT COUNT(*) FROM notifications WHERE user_id = :userId AND is_read = false
     * Request: userId — 조회 사용자 ID / Response: long — 안읽은 알림 수
     *
     * <p>[기존 주석] 안읽음 개수(종 뱃지). 부분 인덱스 idx_notifications_user_unread를 탄다.
     */
    long countByUser_IdAndIsReadFalse(Long userId);

    /**
     * 기능: 내 안읽은 알림 전체를 읽음 처리(벌크 UPDATE)
     * 쿼리: UPDATE notifications SET is_read = true WHERE user_id = :userId AND is_read = false
     * Request: userId — 요청 사용자 ID / Response: int — 갱신된 행 수
     *
     * <p>[기존 주석] 내 안읽음 전체를 읽음 처리(모두 읽음 버튼). 벌크 UPDATE라 영속성 컨텍스트를 우회하므로
     * clearAutomatically로 1차 캐시를 비운다(같은 트랜잭션에서 stale 엔티티 방지).
     *
     * @return 갱신된 행 수
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    int markAllRead(@Param("userId") Long userId);

    /**
     * 기능: 내가 받은 알림을 전부 삭제(탈퇴 시 개인정보 정리용)
     * 쿼리: DELETE FROM notifications WHERE user_id = :userId
     * Request: userId — 대상 사용자 ID / Response: int — 삭제된 행 수
     *
     * <p>벌크 DELETE라 영속성 컨텍스트를 우회하므로 clearAutomatically로 1차 캐시를 비운다
     * (같은 트랜잭션에서 이미 로딩된 엔티티가 삭제 후에도 stale 상태로 남는 것을 막는다).
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Notification n WHERE n.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
