package com.honjeong.notification.dto;

import com.honjeong.notification.domain.NotificationSettings;

/**
 * 알림 수신 설정 응답. 불리언 4개(같이먹기·메이트·공지·이벤트혜택).
 */
public record NotificationSettingsResponse(boolean meal, boolean mate, boolean notice, boolean marketing) {

    public static NotificationSettingsResponse from(NotificationSettings s) {
        return new NotificationSettingsResponse(
                s.isMealEnabled(), s.isMateEnabled(), s.isNoticeEnabled(), s.isMarketingEnabled());
    }
}
