package com.honjeong.checkin.dto;

import java.time.LocalDateTime;

/**
 * 같은 식당 혼밥러 1명. 프라이버시(NFR-03)상 닉네임·시작시각·경과분만 노출한다.
 * userId는 앱이 메이트 프로필로 진입하기 위해 추가로 노출한다(이미 식당에서 공개 체크인한 사용자).
 *
 * @param checkInId      체크인 id
 * @param userId         사용자 id(메이트 프로필 진입용)
 * @param nickname       닉네임
 * @param startedAt      체크인 시작 시각
 * @param elapsedMinutes 경과 분(now − startedAt)
 */
public record CheckInUserResponse(Long checkInId, Long userId, String nickname, LocalDateTime startedAt, long elapsedMinutes) {
}
