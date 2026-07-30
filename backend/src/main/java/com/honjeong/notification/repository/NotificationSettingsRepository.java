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
     * 사용자의 알림 설정 행을 삭제(탈퇴 시 개인정보 정리용).
     *
     * @param userId 대상 사용자 ID
     * @return 삭제된 행 수
     */
    // clearAutomatically 금지 — AccountWithdrawalService.deletePersonalData Javadoc 참조
    @Modifying
    @Query("DELETE FROM NotificationSettings ns WHERE ns.userId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
