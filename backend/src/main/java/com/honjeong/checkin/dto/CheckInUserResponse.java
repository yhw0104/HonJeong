package com.honjeong.checkin.dto;

import java.time.LocalDateTime;

/**
 * 같은 식당 혼밥러 1명. 프라이버시(NFR-03)상 닉네임·시작시각·경과분만 노출하고 정확 좌표·실명·userId는 비노출한다.
 *
 * @param checkInId      체크인 id
 * @param nickname       닉네임
 * @param startedAt      체크인 시작 시각
 * @param elapsedMinutes 경과 분(now − startedAt)
 */
public record CheckInUserResponse(Long checkInId, String nickname, LocalDateTime startedAt, long elapsedMinutes) {
}
