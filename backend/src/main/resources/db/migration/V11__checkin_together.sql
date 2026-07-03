-- 같이먹기 매칭 상태(TOGETHER)·취소(CANCELLED) 지원.
-- 1) 매칭 링크·시각 컬럼
ALTER TABLE check_ins ADD COLUMN matched_at      TIMESTAMP;
ALTER TABLE check_ins ADD COLUMN meal_request_id BIGINT REFERENCES meal_requests(id);

-- 2) 단일 활성 제약을 ACTIVE+TOGETHER로 확장(한 사용자의 현재 활동은 최대 1개)
DROP INDEX IF EXISTS uq_check_ins_active_user;
CREATE UNIQUE INDEX uq_check_ins_current_user
    ON check_ins(user_id) WHERE status IN ('ACTIVE','TOGETHER');

-- 3) TOGETHER TTL 스캔용 부분 인덱스
CREATE INDEX idx_check_ins_together_matched
    ON check_ins(matched_at) WHERE status = 'TOGETHER';
