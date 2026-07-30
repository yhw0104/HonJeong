package com.honjeong.checkin.dto;

import java.time.LocalDateTime;

/**
 * 같은 식당 혼밥러 1명. 프라이버시(NFR-03)상 닉네임·사진·시작시각·경과분만 노출한다.
 * userId는 앱이 메이트 프로필로 진입하기 위해 추가로 노출한다(이미 식당에서 공개 체크인한 사용자).
 *
 * <p>프로필 사진은 2026-07-30에 추가했다 — 목록에서 사진이 안 나와 같은 사람이 메이트 목록과
 * 혼밥러 목록에서 다르게 보였다(사진 없는 사용자는 앱 아이콘이 나온다).
 *
 * @param checkInId       체크인 id
 * @param userId          사용자 id(메이트 프로필 진입용)
 * @param nickname        닉네임
 * @param profileImageUrl 프로필 사진 URL(없으면 null — 앱이 앱 아이콘으로 대체)
 * @param startedAt       체크인 시작 시각
 * @param elapsedMinutes  경과 분(now − startedAt)
 */
public record CheckInUserResponse(Long checkInId, Long userId, String nickname, String profileImageUrl,
        LocalDateTime startedAt, long elapsedMinutes) {
}
