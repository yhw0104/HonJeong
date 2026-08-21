-- 공지사항 추가 — 2026-08-20에 나간 것들(빌드 26~28).
--
-- 실행: ssh honjeong 'sudo docker exec -i honjeong-db psql -U honjeong -d honjeong' < backend/scripts/notices-2026-08-20.sql
--
-- ★공지는 Flyway 마이그레이션이 아니라 이렇게 손으로 넣는다(V15는 테이블만 만든다).
--   내용은 스키마가 아니라 데이터고, 나중에 문구를 고치거나 지울 수 있어야 하기 때문이다.
--
-- ★id를 직접 주지 않는다. 컬럼이 identity라 자동으로 붙고, 지금 시퀀스(17)와 max(id)(17)가
--   맞아떨어져 있어 그대로 18부터 이어진다. 직접 주면 시퀀스가 뒤처져 다음 삽입이 PK 충돌로 죽는다.
--
-- ★published_at이 미래면 목록에서 빠진다(Notice.java의 예약 게시 겸 초안 규칙). now()는 즉시 공개다.
--   DB 타임존이 Asia/Seoul이라 now()가 곧 KST다 — 따로 변환하지 않는다.
--
-- category는 UPDATE | EVENT | GENERAL 셋 중 하나여야 한다(체크 제약). 셋 다 기능 변경이라 UPDATE다.
-- pinned는 두지 않는다 — 상단 고정은 "처음이신가요"(15)와 "커뮤니티 이용규칙"(2) 두 개면 충분하고,
-- 업데이트 소식까지 고정하면 정작 봐야 할 안내가 밀린다.

BEGIN;

INSERT INTO notices (category, title, body, pinned, published_at, created_at, updated_at) VALUES
(
  'UPDATE',
  'Apple로도 로그인할 수 있어요',
  '카카오 외에 Apple 계정으로도 가입하고 로그인할 수 있게 됐어요. 다만 카카오 계정과 Apple 계정은 서로 다른 계정이에요 — 두 곳이 같은 사람인지 확인할 방법을 주지 않아서 자동으로 이어드릴 수가 없어요. 처음 가입하실 때 쓰신 방법으로 로그인해 주세요.',
  false, now(), now(), now()
),
(
  'UPDATE',
  '식당 검색이 가까운 순으로 바뀌었어요',
  '이제 식당을 검색하면 내 위치에서 가까운 곳부터 보여드리고, 결과마다 거리도 함께 표시해요. 위치 권한을 켜두시면 바로 적용되고, 프로필에 "내 동네"를 설정해 두셔도 그 기준으로 정렬돼요. 근처에 없는 가게는 이름으로도 계속 찾을 수 있어요.',
  false, now(), now(), now()
),
(
  'UPDATE',
  '같이 먹기 신청이 오면 탭에 숫자가 떠요',
  '대화 탭처럼, 답을 기다리는 같이 먹기 신청이 있으면 하단 "같이먹기" 탭에 숫자가 표시돼요. 수락하거나 거절하면 바로 사라져요. 그리고 알림 설정에 "뱃지 획득 알림"이 새로 생겨서, 원하지 않으시면 끌 수 있어요.',
  false, now(), now(), now()
);

-- 넣은 것 확인. 세 줄이 최신 순으로 보이면 정상이다.
SELECT id, category, title, pinned, published_at
FROM notices
ORDER BY id DESC
LIMIT 3;

COMMIT;
