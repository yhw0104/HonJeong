package com.honjeong.notification.dto;

/**
 * 안읽음 개수 응답(종 뱃지 폴링용).
 *
 * @param count 안읽은 알림 수
 */
public record UnreadCountResponse(long count) {
}
