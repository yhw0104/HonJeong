package com.honjeong.notification.dto;

import com.honjeong.notification.domain.NotificationSettings;

/**
 * 알림 수신 설정 응답. 불리언 5개(같이먹기·메이트·공지·이벤트혜택·뱃지).
 *
 * <p>응답의 badge는 요청과 달리 원시 boolean이다 — 서버는 언제나 확정된 값을 안다.
 */
public record NotificationSettingsResponse(boolean meal, boolean mate, boolean notice, boolean marketing,
        boolean badge) {

    public static NotificationSettingsResponse from(NotificationSettings s) {
        return new NotificationSettingsResponse(
                s.isMealEnabled(), s.isMateEnabled(), s.isNoticeEnabled(), s.isMarketingEnabled(),
                s.isBadgeEnabled());
    }
}
