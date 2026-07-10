package com.honjeong.notification.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.honjeong.notification.domain.NotificationSettings;

/** 사용자별 알림 설정 리포지토리. 조회/업서트 키는 user_id(유니크). */
public interface NotificationSettingsRepository extends JpaRepository<NotificationSettings, Long> {

    Optional<NotificationSettings> findByUserId(Long userId);
}
