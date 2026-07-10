package com.honjeong.notification.dto;

/**
 * 알림 수신 설정 갱신 요청(PATCH — 4필드 전체 교체).
 */
public record NotificationSettingsRequest(boolean meal, boolean mate, boolean notice, boolean marketing) {
}
