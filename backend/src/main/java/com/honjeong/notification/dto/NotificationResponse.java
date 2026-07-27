package com.honjeong.notification.dto;

import java.time.LocalDateTime;
import com.honjeong.global.common.DisplayNames;
import com.honjeong.notification.domain.Notification;

/**
 * 알림 목록 한 건. 문구는 내려주지 않는다 — 앱이 type + actorNickname으로 조립한다.
 *
 * @param id            알림 id
 * @param type          알림 종류(enum 이름 문자열)
 * @param actorNickname 알림 주체 닉네임(주체 없는 알림이면 null, 탈퇴한 사용자면 '알 수 없음')
 * @param isRead        읽음 여부
 * @param createdAt     발생 시각
 */
public record NotificationResponse(Long id, String type, String actorNickname, boolean isRead,
        LocalDateTime createdAt) {

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getType().name(),
                // actor 자체가 없는 경우(BADGE_EARNED 등)는 null 그대로 유지하고,
                // 탈퇴자는 닉네임이 null이라 '알 수 없음'으로 표시한다(DisplayNames).
                n.getActor() == null ? null : DisplayNames.nicknameOrUnknown(n.getActor().getNickname()),
                n.isRead(), n.getCreatedAt());
    }
}
