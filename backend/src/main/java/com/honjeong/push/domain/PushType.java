package com.honjeong.push.domain;

import com.honjeong.notification.domain.NotificationType;

/**
 * 푸시 배너의 종류. 앱이 이 값으로 이동할 화면과 무효화할 캐시를 정한다.
 *
 * <p><b>{@link NotificationType}과 일부러 분리된 enum이다.</b> NotificationType은
 * {@code notifications.type} 컬럼에 저장되는 값인데 CHAT_MESSAGE는 절대 저장되지 않는다
 * (채팅은 알림함에 쌓지 않기로 결정 — 메시지마다 쌓으면 알림함이 도배돼 드문 사건이 묻힌다).
 * 저장 enum에 저장되지 않는 값을 섞으면 "알림함에 CHAT_MESSAGE 행이 있을 수 있다"는
 * 거짓 신호가 되고, NotificationSettingsService.isEnabled의 exhaustive switch도 깨진다.
 */
public enum PushType {
    MEAL_REQUEST_RECEIVED,
    MEAL_REQUEST_ACCEPTED,
    MEAL_MATCH_CANCELLED,
    MATE_REQUEST_RECEIVED,
    MATE_REQUEST_ACCEPTED,
    BADGE_EARNED,
    /** 채팅 새 메시지 — 알림함에 저장되지 않는 푸시 전용 종류. */
    CHAT_MESSAGE;

    /**
     * 알림함 종류를 푸시 종류로 사상한다. 이름이 1:1로 같다.
     *
     * @param type 알림함 종류
     * @return 같은 이름의 푸시 종류
     */
    public static PushType from(NotificationType type) {
        return valueOf(type.name());
    }
}
