CREATE TABLE conversations (
    id                BIGSERIAL PRIMARY KEY,
    meal_request_id   BIGINT NOT NULL UNIQUE REFERENCES meal_requests(id),
    place_id          BIGINT NOT NULL REFERENCES places(id),
    from_user_id      BIGINT NOT NULL REFERENCES users(id),
    to_user_id        BIGINT NOT NULL REFERENCES users(id),
    status            VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_message_at   TIMESTAMP,
    from_last_read_at TIMESTAMP,
    to_last_read_at   TIMESTAMP,
    created_at        TIMESTAMP NOT NULL,
    updated_at        TIMESTAMP NOT NULL
);
CREATE INDEX idx_conversations_from ON conversations(from_user_id, last_message_at);
CREATE INDEX idx_conversations_to   ON conversations(to_user_id,   last_message_at);

CREATE TABLE chat_messages (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES conversations(id),
    sender_user_id  BIGINT NOT NULL REFERENCES users(id),
    type            VARCHAR(10) NOT NULL,
    text            VARCHAR(1000),
    image_url       VARCHAR(500),
    created_at      TIMESTAMP NOT NULL
);
CREATE INDEX idx_chat_messages_conv ON chat_messages(conversation_id, id);

COMMENT ON TABLE conversations IS '매칭(meal_request) 1:1 대화방. ACTIVE=전송가능, CLOSED=읽기전용(영구보관)';
COMMENT ON TABLE chat_messages IS '매칭 대화 메시지(TEXT|IMAGE)';
