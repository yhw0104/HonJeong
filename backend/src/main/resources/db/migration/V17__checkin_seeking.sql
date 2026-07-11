-- 모집중(SEEKING) 상태 추가: 체크인 정문을 SEEKING으로 이동, 혼밥중(ACTIVE)은 매칭 실패 폴백.
-- 단일 활성 제약을 SEEKING+ACTIVE+TOGETHER로 확장(한 사용자의 현재 활동은 최대 1개).
DROP INDEX IF EXISTS uq_check_ins_current_user;
CREATE UNIQUE INDEX uq_check_ins_current_user
    ON check_ins(user_id) WHERE status IN ('SEEKING','ACTIVE','TOGETHER');

COMMENT ON COLUMN check_ins.status IS
    'SEEKING(모집중·정문)|ACTIVE(혼밥중·매칭실패폴백)|TOGETHER(같이먹는중)|ENDED(종료)|CANCELLED(취소·집계제외)';
