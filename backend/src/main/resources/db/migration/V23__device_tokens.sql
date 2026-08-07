-- FCM 기기 토큰. 한 사용자가 기기를 여러 대 쓸 수 있으므로 행-per-기기다.
-- 토큰은 "기기"에 붙는 값이라 주인이 바뀔 수 있다(한 폰을 두 사람이 번갈아 쓰는 경우) →
-- token에 UNIQUE를 걸고 등록은 UPSERT(주인 갱신)로 처리한다. 안 그러면 이전 사용자의
-- 알림이 다음 사용자 폰에 계속 뜬다.
CREATE TABLE device_tokens (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users(id),
    token        VARCHAR(255) NOT NULL UNIQUE,
    platform     VARCHAR(16)  NOT NULL,
    last_used_at TIMESTAMP,
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP    NOT NULL,
    CONSTRAINT ck_device_tokens_platform CHECK (platform IN ('IOS', 'ANDROID'))
);

CREATE INDEX idx_device_tokens_user_id ON device_tokens(user_id);

COMMENT ON TABLE  device_tokens               IS 'FCM 기기 토큰(행-per-기기)';
COMMENT ON COLUMN device_tokens.token         IS 'FCM 등록 토큰. UNIQUE — 기기가 주인을 바꾸면 user_id를 갱신한다';
COMMENT ON COLUMN device_tokens.platform      IS 'IOS | ANDROID. CHECK 제약 — 오타가 enum 역직렬화를 깨면 조회 API 전체가 500이 된다';
COMMENT ON COLUMN device_tokens.last_used_at  IS '마지막 발송 성공 시각(NULL=아직 발송한 적 없음)';
