-- ============================================================================
-- cleanup-review-testdata.sql — seed-review-testdata.sql 가 넣은 시드만 삭제.
-- 실제(공공데이터, source<>'SEED') 식당·실유저는 건드리지 않는다.
-- 대상: source='SEED' 식당 + me폰(01012345678)/시드 닉네임 유저 + 그 의존행(FK child-first).
--
-- 실행: docker exec -i honjeong-db psql -U honjeong -d honjeong < scripts/cleanup-review-testdata.sql
-- ============================================================================
DO $$
DECLARE
    seed_nicks text[] := ARRAY['연남혼밥러','혼밥3년차','연남주민','국밥러버'];
    seed_uids  bigint[] := ARRAY(SELECT id FROM users  WHERE phone = '01012345678' OR nickname = ANY(seed_nicks));
    seed_pids  bigint[] := ARRAY(SELECT id FROM places WHERE source = 'SEED');
BEGIN
    DELETE FROM review_tags rt USING reviews rv
        WHERE rt.review_id = rv.id AND (rv.user_id = ANY(seed_uids) OR rv.place_id = ANY(seed_pids));
    DELETE FROM reviews       WHERE user_id = ANY(seed_uids) OR place_id = ANY(seed_pids);
    DELETE FROM meal_requests
        WHERE from_user_id = ANY(seed_uids) OR place_id = ANY(seed_pids)
           OR to_check_in_id IN (SELECT id FROM check_ins WHERE user_id = ANY(seed_uids) OR place_id = ANY(seed_pids));
    DELETE FROM check_ins             WHERE user_id = ANY(seed_uids) OR place_id = ANY(seed_pids);
    DELETE FROM refresh_tokens        WHERE user_id = ANY(seed_uids);
    DELETE FROM social_accounts       WHERE user_id = ANY(seed_uids);
    DELETE FROM terms_agreements      WHERE user_id = ANY(seed_uids);
    DELETE FROM user_food_preferences WHERE user_id = ANY(seed_uids);
    DELETE FROM users  WHERE id = ANY(seed_uids);
    DELETE FROM places WHERE id = ANY(seed_pids);

    RAISE NOTICE '시드 삭제 완료 — users %, places %', cardinality(seed_uids), cardinality(seed_pids);
END $$;
