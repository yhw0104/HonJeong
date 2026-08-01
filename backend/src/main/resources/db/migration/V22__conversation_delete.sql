-- 대화방 소프트 삭제(참여자별). 내 목록에서만 숨기고 chat_messages는 보존한다 —
-- 신고·차단 조사에 대비하기 위함. 참여자가 from/to 둘뿐이라 컬럼 2개로 충분하다.
ALTER TABLE conversations
    ADD COLUMN from_deleted_at TIMESTAMP,
    ADD COLUMN to_deleted_at   TIMESTAMP;

COMMENT ON COLUMN conversations.from_deleted_at IS 'from_user가 내 목록에서 삭제한 시각(NULL=안 지움)';
COMMENT ON COLUMN conversations.to_deleted_at   IS 'to_user가 내 목록에서 삭제한 시각(NULL=안 지움)';
