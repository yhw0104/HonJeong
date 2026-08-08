-- 기기 식별자. 토큰은 갱신될 때마다 값이 바뀌지만 설치 ID는 앱을 지울 때까지 그대로다.
--
-- 왜 필요한가: 로그아웃은 ①서버 삭제 ②FCM 폐기 두 단계로 토큰을 끊는데, 둘 다 실패하면
-- 그 토큰은 FCM에 살아 있고 DB에도 남아 있는데 기기에는 없다 — 다시는 지목해 지울 수 없다.
-- 그 폰을 넘겨받은 사람의 잠금화면에 이전 사용자의 알림이 계속 뜬다. 설치 ID가 있으면
-- "같은 기기의 다른 토큰"으로 지목할 수 있어, 누가 로그인하든 등록 시점에 정리된다.
-- (V26 staleness 청소는 이걸 60일 뒤에야 하는 백스톱이다.)
--
-- ★ NULL 허용이고 UNIQUE도 아니다:
--   - NULL: 서버가 앱보다 먼저 배포된다. 이미 나가 있는 빌드는 설치 ID를 보내지 않는다.
--   - not UNIQUE: 새 토큰을 넣고 옛 토큰을 지우는 사이 같은 설치 ID의 행이 잠시 둘이 된다.
ALTER TABLE device_tokens ADD COLUMN installation_id VARCHAR(64);

CREATE INDEX idx_device_tokens_installation_id ON device_tokens(installation_id);

COMMENT ON COLUMN device_tokens.installation_id IS
    '앱 설치 하나를 가리키는 불투명 식별자(앱이 생성·보관). 토큰 갱신에도 안 바뀐다. NULL=설치 ID를 보내지 않는 구버전 앱';
