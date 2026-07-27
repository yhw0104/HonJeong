package com.honjeong.notification.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.honjeong.notification.domain.NotificationSettings;

/** 사용자별 알림 설정 리포지토리. 조회/업서트 키는 user_id(유니크). */
public interface NotificationSettingsRepository extends JpaRepository<NotificationSettings, Long> {

    Optional<NotificationSettings> findByUserId(Long userId);

    /**
     * 기능: 사용자의 알림 설정 행을 삭제(탈퇴 시 개인정보 정리용)
     * 쿼리: DELETE FROM notification_settings WHERE user_id = :userId
     * Request: userId — 대상 사용자 ID / Response: int — 삭제된 행 수
     *
     * <p>벌크 DELETE라 영속성 컨텍스트를 우회하므로 clearAutomatically로 1차 캐시를 비운다
     * (같은 트랜잭션에서 이미 로딩된 엔티티가 삭제 후에도 stale 상태로 남는 것을 막는다).
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM NotificationSettings ns WHERE ns.userId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
