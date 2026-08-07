-- 대화방별 알림 끄기(참여자별). 알림 설정 화면은 같이먹기·메이트처럼 드문 사건만 담당하고,
-- 채팅은 대화 단위로 끈다(카카오톡과 같은 방식). V22의 참여자별 소프트 삭제와 같은 패턴이다.
ALTER TABLE conversations
    ADD COLUMN from_muted BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN to_muted   BOOLEAN NOT NULL DEFAULT false;

COMMENT ON COLUMN conversations.from_muted IS 'from_user가 이 대화의 푸시를 껐는가(false=받음)';
COMMENT ON COLUMN conversations.to_muted   IS 'to_user가 이 대화의 푸시를 껐는가(false=받음)';
