package com.honjeong.notification.dto;

import java.time.LocalDateTime;
import com.honjeong.notification.domain.Notification;

/**
 * 알림 목록 한 건. 문구는 내려주지 않는다 — 앱이 type + actorNickname으로 조립한다.
 *
 * @param id            알림 id
 * @param type          알림 종류(enum 이름 문자열)
 * @param actorNickname 알림 주체 닉네임(탈퇴 등으로 없으면 null)
 * @param isRead        읽음 여부
 * @param createdAt     발생 시각
 */
public record NotificationResponse(Long id, String type, String actorNickname, boolean isRead,
        LocalDateTime createdAt) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getType().name(),
                n.getActor() == null ? null : n.getActor().getNickname(), n.isRead(), n.getCreatedAt());
    }
}
