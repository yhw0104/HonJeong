-- last_used_at의 뜻을 실제 동작에 맞춘다. V23은 '마지막 발송 성공 시각'이라고 적었지만,
-- PushSender.send는 '영구 무효' 토큰 목록만 돌려주므로 일시 실패(UNAVAILABLE·INTERNAL·
-- QUOTA_EXCEEDED·INVALID_ARGUMENT)와 성공을 구분할 수 없다 — 그 토큰들도 시각이 갱신된다.
-- 지금은 읽는 곳이 없지만, 나중에 '미사용 토큰 정리'를 붙일 때 이 차이를 모르면 죽은 토큰이
-- 영원히 살아 있는 것으로 보인다. (V23은 이미 적용돼 있어 수정하지 못하므로 여기서 덮어쓴다.)
COMMENT ON COLUMN device_tokens.last_used_at IS '마지막 발송 시도 시각(성공 보장 아님, NULL=아직 시도한 적 없음)';
