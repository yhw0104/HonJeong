-- 부하 테스트용 가상 사용자와 그들이 만든 데이터를 전부 지운다.
--
--   set -a; . ./.env; set +a
--   docker exec -i -e PGPASSWORD="$DB_PASSWORD" honjeong-db \
--     psql -U honjeong -d honjeong -v ON_ERROR_STOP=1 < loadtest/cleanup-users.sql
--
-- ★로컬 DB에서만 쓸 것. 대상은 social_accounts.provider_user_id가
--   'mock-kakao-loadtest-%'인 사용자뿐이다 — seed-users.sh가 idToken을 'loadtest-N'으로 주고
--   MockOAuthVerifier가 'mock-kakao-' 접두사를 붙여 만든 식별자다. 실제 카카오 사용자는
--   숫자 id라 이 패턴에 걸리지 않는다.
--
-- ★삭제 순서가 중요하다. users를 참조하는 FK 중 ON DELETE CASCADE가 붙은 것은 일부뿐이라
--   (reviews·device_tokens·conversations·check_ins 등은 CASCADE가 없다) 자식부터 지워야 한다.
BEGIN;

CREATE TEMP TABLE victims ON COMMIT DROP AS
SELECT DISTINCT u.id
FROM users u
JOIN social_accounts sa ON sa.user_id = u.id
WHERE sa.provider_user_id LIKE 'mock-kakao-loadtest-%';

\echo '지울 사용자 수:'
SELECT count(*) FROM victims;

-- 리뷰 사진 → 태그 → 리뷰 (review_photos는 reviews에 CASCADE지만 명시적으로 둔다)
DELETE FROM review_photos WHERE review_id IN (SELECT id FROM reviews WHERE user_id IN (SELECT id FROM victims));
DELETE FROM review_tags   WHERE review_id IN (SELECT id FROM reviews WHERE user_id IN (SELECT id FROM victims));
DELETE FROM reviews       WHERE user_id IN (SELECT id FROM victims);

-- 대화: 메시지가 conversations와 users를 모두 참조하므로 메시지부터
DELETE FROM chat_messages WHERE sender_user_id IN (SELECT id FROM victims)
   OR conversation_id IN (SELECT id FROM conversations
                          WHERE from_user_id IN (SELECT id FROM victims)
                             OR to_user_id   IN (SELECT id FROM victims));
DELETE FROM conversations WHERE from_user_id IN (SELECT id FROM victims)
                             OR to_user_id   IN (SELECT id FROM victims);

-- 같이먹기 신청은 check_ins를 참조하므로 체크인보다 먼저
DELETE FROM meal_requests WHERE from_user_id IN (SELECT id FROM victims)
   OR to_check_in_id IN (SELECT id FROM check_ins WHERE user_id IN (SELECT id FROM victims));
DELETE FROM check_ins WHERE user_id IN (SELECT id FROM victims);

-- ★즐겨찾기(favorites)에는 user_id가 없다. 그룹(favorite_groups)에만 있고 favorites는
--   group_id로 그룹을 참조한다. 그래서 사용자 → 그룹 → 즐겨찾기 순으로 타고 들어가야 한다.
--   (처음엔 favorites.user_id로 썼다가 "column user_id does not exist"로 트랜잭션이 통째로
--    롤백됐다 — ON_ERROR_STOP=1과 BEGIN 덕분에 반쯤 지워진 상태가 되지는 않았다.)
DELETE FROM favorites WHERE group_id IN (
    SELECT id FROM favorite_groups WHERE user_id IN (SELECT id FROM victims));
DELETE FROM favorite_groups WHERE user_id IN (SELECT id FROM victims);

-- 나머지 사용자 부속 데이터
DELETE FROM notifications        WHERE user_id IN (SELECT id FROM victims) OR actor_user_id IN (SELECT id FROM victims);
DELETE FROM notification_settings WHERE user_id IN (SELECT id FROM victims);
DELETE FROM user_badges          WHERE user_id IN (SELECT id FROM victims);
DELETE FROM device_tokens        WHERE user_id IN (SELECT id FROM victims);
DELETE FROM user_food_preferences WHERE user_id IN (SELECT id FROM victims);
DELETE FROM mates                WHERE user_id IN (SELECT id FROM victims) OR mate_user_id IN (SELECT id FROM victims);
DELETE FROM mate_requests        WHERE from_user_id IN (SELECT id FROM victims) OR to_user_id IN (SELECT id FROM victims);
DELETE FROM blocks               WHERE blocker_id IN (SELECT id FROM victims) OR blocked_id IN (SELECT id FROM victims);
DELETE FROM reports              WHERE reporter_id IN (SELECT id FROM victims);
DELETE FROM terms_agreements     WHERE user_id IN (SELECT id FROM victims);
DELETE FROM refresh_tokens       WHERE user_id IN (SELECT id FROM victims);
DELETE FROM social_accounts      WHERE user_id IN (SELECT id FROM victims);

DELETE FROM users WHERE id IN (SELECT id FROM victims);

COMMIT;

\echo '남은 loadtest 사용자(0이어야 정상):'
SELECT count(*) FROM social_accounts WHERE provider_user_id LIKE 'mock-kakao-loadtest-%';
