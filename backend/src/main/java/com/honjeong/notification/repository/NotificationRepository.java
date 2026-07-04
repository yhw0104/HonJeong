package com.honjeong.notification.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.honjeong.notification.domain.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 내 알림 최근 30건(보존기간 이후분). actor를 EntityGraph로 함께 로드해
     * DTO 변환 시 닉네임 접근 N+1을 막는다.
     */
    @EntityGraph(attributePaths = "actor")
    List<Notification> findTop30ByUser_IdAndCreatedAtAfterOrderByCreatedAtDesc(Long userId, LocalDateTime after);

    /** 안읽음 개수(종 뱃지). 부분 인덱스 idx_notifications_user_unread를 탄다. */
    long countByUser_IdAndIsReadFalse(Long userId);

    /**
     * 내 안읽음 전체를 읽음 처리(모두 읽음 버튼). 벌크 UPDATE라 영속성 컨텍스트를 우회하므로
     * clearAutomatically로 1차 캐시를 비운다(같은 트랜잭션에서 stale 엔티티 방지).
     *
     * @return 갱신된 행 수
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    int markAllRead(@Param("userId") Long userId);
}
