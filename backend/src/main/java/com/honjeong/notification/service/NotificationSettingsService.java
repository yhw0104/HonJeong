package com.honjeong.notification.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.honjeong.notification.domain.NotificationSettings;
import com.honjeong.notification.domain.NotificationType;
import com.honjeong.notification.dto.NotificationSettingsRequest;
import com.honjeong.notification.dto.NotificationSettingsResponse;
import com.honjeong.notification.repository.NotificationSettingsRepository;

/**
 * 1. 기능: 사용자별 알림 수신 설정 조회/갱신 + 발행 게이팅용 판정(isEnabled)
 * 2. 사용 Controller: NotificationController(get/update), NotificationService(publish 게이팅)
 *
 * <p>행이 없으면 기본값으로 간주한다(lazy — 백필 불필요). 저장은 upsert(없으면 기본값 행 생성 후 갱신).
 */
@Service
public class NotificationSettingsService {

    private final NotificationSettingsRepository settingsRepository;

    public NotificationSettingsService(NotificationSettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    /** 내 알림 설정. 행이 없으면 기본값. */
    @Transactional(readOnly = true)
    public NotificationSettingsResponse getSettings(Long userId) {
        return NotificationSettingsResponse.from(
                settingsRepository.findByUserId(userId).orElseGet(() -> NotificationSettings.of(userId)));
    }

    /** 알림 설정 업서트(없으면 기본값 행 생성 후 요청값으로 갱신). */
    @Transactional
    public NotificationSettingsResponse updateSettings(Long userId, NotificationSettingsRequest req) {
        NotificationSettings s = settingsRepository.findByUserId(userId)
                .orElseGet(() -> NotificationSettings.of(userId));
        s.update(req.meal(), req.mate(), req.notice(), req.marketing());
        return NotificationSettingsResponse.from(settingsRepository.save(s));
    }

    /**
     * 발행 게이팅 판정: 수신자가 이 종류 알림을 켜 두었는가.
     * 행이 없으면 기본값(meal/mate ON). 타입→필드 매핑은 switch로 강제(새 타입 추가 시 컴파일 실패로 알림).
     */
    @Transactional(readOnly = true)
    public boolean isEnabled(Long userId, NotificationType type) {
        var opt = settingsRepository.findByUserId(userId);
        return switch (type) {
            case MEAL_REQUEST_RECEIVED, MEAL_REQUEST_ACCEPTED, MEAL_MATCH_CANCELLED ->
                    opt.map(NotificationSettings::isMealEnabled).orElse(true);
            case MATE_REQUEST_RECEIVED, MATE_REQUEST_ACCEPTED ->
                    opt.map(NotificationSettings::isMateEnabled).orElse(true);
        };
    }
}
