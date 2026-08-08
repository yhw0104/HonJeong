-- last_used_at은 '마지막 발송 시도' 시각이다(V25 주석 참조). 그래서 청소 기준으로 쓸 수 없다 --
-- 지우려는 고아 토큰은 지금도 계속 발송되고 있어서 매번 시각이 새로 찍히고, 영원히 신선해 보인다.
-- 그래서 '앱이 등록한 시각'만 담는 칸을 따로 둔다. 갱신 주체는 등록 UPSERT 하나뿐이다.
ALTER TABLE device_tokens ADD COLUMN last_registered_at TIMESTAMP;

-- 백필 = 알려진 마지막 활동의 가장 정직한 근사. now()로 채우면 안전하지만 배포 후 60일간
-- 청소가 아무 일도 하지 않는다.
UPDATE device_tokens SET last_registered_at = COALESCE(last_used_at, created_at);

-- 백필이 모든 행을 채운 뒤에 NOT NULL을 건다. 새 등록 경로가 이 칸을 빼먹는 것을 DB가 막는다.
ALTER TABLE device_tokens ALTER COLUMN last_registered_at SET NOT NULL;

CREATE INDEX idx_device_tokens_last_registered_at ON device_tokens(last_registered_at);

COMMENT ON COLUMN device_tokens.last_registered_at IS
    '앱이 이 토큰을 등록·재등록한 마지막 시각. 발송은 갱신하지 않는다 — staleness 청소의 판단 기준';
