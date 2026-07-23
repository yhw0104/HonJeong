-- 뱃지 지급 도메인. 사용자가 획득한 뱃지를 '행 하나'로 저장한다(획득분만 저장, 인당 최대 10행).
-- badge_key는 서버 BadgeCatalog enum 상수명(예: 'SOLO_50'). 획득 시각 earned_at 보존.
-- 유니크 (user_id, badge_key)로 같은 뱃지 중복 지급을 막는다(멱등 지급의 뿌리).
CREATE TABLE user_badges (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    badge_key  VARCHAR(32) NOT NULL,
    earned_at  TIMESTAMP NOT NULL,
    CONSTRAINT uq_user_badge UNIQUE (user_id, badge_key)
);
CREATE INDEX idx_user_badges_user ON user_badges(user_id);
COMMENT ON TABLE user_badges IS '사용자가 획득한 뱃지(행-per-뱃지, 획득분만 저장)';
COMMENT ON COLUMN user_badges.user_id IS '획득한 사용자';
COMMENT ON COLUMN user_badges.badge_key IS '뱃지 식별자(BadgeCatalog enum 상수명)';
COMMENT ON COLUMN user_badges.earned_at IS '획득 시각(KST)';
