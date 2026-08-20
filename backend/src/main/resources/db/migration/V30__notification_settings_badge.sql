-- 뱃지 획득 알림 수신 설정. 예전에는 NotificationSettingsService.isEnabled가
-- BADGE_EARNED를 true로 못 박고 있어 사용자가 끌 방법이 아예 없었다.
--
-- 기본값 true인 이유: 지금까지 모든 사용자가 이 알림을 받아 왔다. 기본을 false로 두면
-- 이 마이그레이션이 도는 순간 전원의 뱃지 알림이 조용히 꺼진다 — 아무도 끄지 않았는데
-- 꺼지고, 알림이 안 온다는 사실은 한참 뒤에나 드러난다. 기존 동작을 그대로 옮긴다.
-- (marketing만 기본 false인 것은 광고성 정보라 옵트인이어야 하기 때문이고, 뱃지는 다르다.)
ALTER TABLE notification_settings
    ADD COLUMN badge_enabled BOOLEAN NOT NULL DEFAULT TRUE;
