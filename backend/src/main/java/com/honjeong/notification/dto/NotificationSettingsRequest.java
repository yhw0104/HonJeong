package com.honjeong.notification.dto;

/**
 * 알림 수신 설정 갱신 요청.
 *
 * <p>★{@code badge}만 {@code Boolean}이고 나머지는 원시 boolean이다. 나머지 넷은 이 API가
 * 처음 생길 때부터 있었지만 badge는 나중에 붙었고, <b>이미 배포된 앱(1.0.0 빌드 26)은 넷만
 * 보낸다</b>. badge를 원시 boolean으로 두면 없는 값이 Jackson 기본값 false로 채워져, 구버전
 * 앱에서 토글을 아무거나 하나 건드리는 순간 뱃지 알림이 조용히 꺼진다.
 * null은 "안 보냈다"는 뜻이고, 그때는 서버가 기존 값을 유지한다
 * ({@code NotificationSettings#update}).
 */
public record NotificationSettingsRequest(boolean meal, boolean mate, boolean notice, boolean marketing,
        Boolean badge) {
}
