-- ============================================================================
-- seed-review-testdata.sql — 혼밥 인증/리뷰(FR-201) 수동 검증용 시드 데이터
--
-- 전제: V1~V6 마이그레이션이 적용된 로컬 honjeong-db (= 한 번 ./gradlew bootRun 한 뒤).
-- 재실행 안전: 맨 앞에서 이전 시드(source='SEED' 식당 + 시드 유저)를 FK 순서로 지우고 다시 넣는다.
--
-- 로그인: 앱 휴대폰 로그인에서 phone "01012345678" + 인증번호 "000000" → '연남혼밥러'(me)로 로그인.
--   (mock SMS 고정코드 000000, 기존 ACTIVE 유저면 온보딩 없이 바로 로그인)
--
-- 적재: docker exec -i honjeong-db psql -U honjeong -d honjeong < scripts/seed-review-testdata.sql
-- 주의: places는 공공데이터 마스터 스키마라 키가 (source, source_id). 시드 식당은 source='SEED'.
-- ============================================================================

DO $$
DECLARE
    base   timestamp := (now() AT TIME ZONE 'Asia/Seoul');  -- 서버 KST LocalDateTime 컨벤션과 정렬
    me     bigint; u2 bigint; u3 bigint; u4 bigint;
    p1     bigint; p2 bigint; p3 bigint; p4 bigint; p5 bigint;
    c_a    bigint; c_b bigint; c_c bigint; c_d bigint; c_e bigint;  -- me 체크인
    c_u2   bigint;                                                   -- user2 인증 체크인
    r      bigint;
    seed_nicks text[] := ARRAY['연남혼밥러','혼밥3년차','연남주민','국밥러버'];
    seed_uids  bigint[];
    seed_pids  bigint[];
BEGIN
    -- 0) 이전 시드 정리 (FK child-first) -------------------------------------
    -- 시드 대상 = 닉네임 매칭 + me 폰(01012345678)으로 이미 만들어진 유저까지 포함
    seed_uids := ARRAY(SELECT id FROM users WHERE phone = '01012345678' OR nickname = ANY(seed_nicks));
    seed_pids := ARRAY(SELECT id FROM places WHERE source = 'SEED');

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

    -- 1) 유저 (me + 리뷰 작성자 3명) -----------------------------------------
    INSERT INTO users(phone, nickname, status, region, region_lat, region_lng, allow_meal_request, created_at, updated_at)
        VALUES ('01012345678','연남혼밥러','ACTIVE','마포구 연남동',37.5605,126.9255,true, base, base)
        RETURNING id INTO me;
    INSERT INTO users(nickname, status, allow_meal_request, created_at, updated_at)
        VALUES ('혼밥3년차','ACTIVE',true, base, base) RETURNING id INTO u2;
    INSERT INTO users(nickname, status, allow_meal_request, created_at, updated_at)
        VALUES ('연남주민','ACTIVE',true, base, base) RETURNING id INTO u3;
    INSERT INTO users(nickname, status, allow_meal_request, created_at, updated_at)
        VALUES ('국밥러버','ACTIVE',true, base, base) RETURNING id INTO u4;

    -- 2) 식당 5곳 (연남동 일대) — source='SEED', source_id=p1..p5 ------------
    INSERT INTO places(source, source_id, name, address, road_address, latitude, longitude, category, phone, created_at, updated_at)
        VALUES ('SEED','p1','큰순두부 연남점','서울 마포구 연남동 227-15','서울 마포구 연남로 31',37.5605,126.9255,'한식','02-322-1014', base, base) RETURNING id INTO p1;
    INSERT INTO places(source, source_id, name, address, road_address, latitude, longitude, category, phone, created_at, updated_at)
        VALUES ('SEED','p2','연남 김밥','서울 마포구 연남동 240-3','서울 마포구 동교로 245',37.5612,126.9248,'분식',NULL, base, base) RETURNING id INTO p2;
    INSERT INTO places(source, source_id, name, address, road_address, latitude, longitude, category, phone, created_at, updated_at)
        VALUES ('SEED','p3','혼밥의자','서울 마포구 연남동 390-1','서울 마포구 성미산로 199',37.5598,126.9261,'한식',NULL, base, base) RETURNING id INTO p3;
    INSERT INTO places(source, source_id, name, address, road_address, latitude, longitude, category, phone, created_at, updated_at)
        VALUES ('SEED','p4','옥상국밥','서울 마포구 연남동 384-7','서울 마포구 성미산로 161',37.5620,126.9239,'한식',NULL, base, base) RETURNING id INTO p4;
    INSERT INTO places(source, source_id, name, address, road_address, latitude, longitude, category, phone, created_at, updated_at)
        VALUES ('SEED','p5','연남 파스타바','서울 마포구 연남동 229-12','서울 마포구 연남로1길 45',37.5589,126.9270,'양식',NULL, base, base) RETURNING id INTO p5;

    -- 3) me 체크인 5건 (3건은 리뷰 있음, 2건은 '일기 없음') -------------------
    INSERT INTO check_ins(user_id, place_id, status, started_at, ended_at, created_at)
        VALUES (me, p1, 'ENDED', base - interval '5 days',  base - interval '5 days'  + interval '40 min', base - interval '5 days')  RETURNING id INTO c_a; -- 이번달, 리뷰 없음
    INSERT INTO check_ins(user_id, place_id, status, started_at, ended_at, created_at)
        VALUES (me, p3, 'ENDED', base - interval '13 days', base - interval '13 days' + interval '35 min', base - interval '13 days') RETURNING id INTO c_b; -- 이번달, 리뷰 있음
    INSERT INTO check_ins(user_id, place_id, status, started_at, ended_at, created_at)
        VALUES (me, p1, 'ENDED', base - interval '34 days', base - interval '34 days' + interval '50 min', base - interval '34 days') RETURNING id INTO c_c; -- 지난달, 리뷰 있음
    INSERT INTO check_ins(user_id, place_id, status, started_at, ended_at, created_at)
        VALUES (me, p4, 'ENDED', base - interval '40 days', base - interval '40 days' + interval '25 min', base - interval '40 days') RETURNING id INTO c_d; -- 지난달, 리뷰 있음
    INSERT INTO check_ins(user_id, place_id, status, started_at, ended_at, created_at)
        VALUES (me, p2, 'ENDED', base - interval '62 days', base - interval '62 days' + interval '20 min', base - interval '62 days') RETURNING id INTO c_e; -- 2달전, 리뷰 없음

    -- 다른 유저 체크인: user2 인증용 ENDED 1건 + user3/user4 '지금 혼밥 중' ACTIVE 2건
    INSERT INTO check_ins(user_id, place_id, status, started_at, ended_at, created_at)
        VALUES (u2, p1, 'ENDED', base - interval '10 days', base - interval '10 days' + interval '30 min', base - interval '10 days') RETURNING id INTO c_u2;
    INSERT INTO check_ins(user_id, place_id, status, started_at, created_at)
        VALUES (u3, p1, 'ACTIVE', base - interval '20 min', base - interval '20 min');
    INSERT INTO check_ins(user_id, place_id, status, started_at, created_at)
        VALUES (u4, p1, 'ACTIVE', base - interval '45 min', base - interval '45 min');

    -- 4) 리뷰 + 친화태그 ----------------------------------------------------
    -- me: 3건 (위 c_b, c_c, c_d 에 대응)
    INSERT INTO reviews(user_id, check_in_id, place_id, visited_at, content, taste_rating, solo_friendly_rating, created_at, updated_at)
        VALUES (me, c_b, p3, base - interval '13 days', '바테이블 끝자리. 책 읽으며 30분, 아무도 신경 안 씀.', 4, 4, base - interval '13 days', base - interval '13 days') RETURNING id INTO r;
    INSERT INTO review_tags(review_id, place_id, tag) VALUES (r, p3, '바테이블'), (r, p3, '오래 OK');

    INSERT INTO reviews(user_id, check_in_id, place_id, visited_at, content, taste_rating, solo_friendly_rating, created_at, updated_at)
        VALUES (me, c_c, p1, base - interval '34 days', '벽 보고 앉아서 마음 편히 순두부 한 그릇.', 5, 5, base - interval '34 days', base - interval '34 days') RETURNING id INTO r;
    INSERT INTO review_tags(review_id, place_id, tag) VALUES (r, p1, '1인석 많음'), (r, p1, '눈치 없음');

    INSERT INTO reviews(user_id, check_in_id, place_id, visited_at, content, taste_rating, solo_friendly_rating, created_at, updated_at)
        VALUES (me, c_d, p4, base - interval '40 days', '점심 빠르게. 1인석 바로 앉음.', 4, 4, base - interval '40 days', base - interval '40 days') RETURNING id INTO r;
    INSERT INTO review_tags(review_id, place_id, tag) VALUES (r, p4, '1인석 많음');

    -- user2: p1 인증리뷰(체크인 연결) + p3 일반리뷰
    INSERT INTO reviews(user_id, check_in_id, place_id, visited_at, content, taste_rating, solo_friendly_rating, created_at, updated_at)
        VALUES (u2, c_u2, p1, base - interval '10 days', '3년째 단골. 혼밥하기 진짜 편한 집.', 5, 5, base - interval '10 days', base - interval '10 days') RETURNING id INTO r;
    INSERT INTO review_tags(review_id, place_id, tag) VALUES (r, p1, '1인석 많음'), (r, p1, '눈치 없음'), (r, p1, '바테이블');

    INSERT INTO reviews(user_id, check_in_id, place_id, visited_at, content, taste_rating, solo_friendly_rating, created_at, updated_at)
        VALUES (u2, NULL, p3, base - interval '8 days', '바테이블이 좋아요. 음식은 보통.', 4, 3, base - interval '8 days', base - interval '8 days') RETURNING id INTO r;
    INSERT INTO review_tags(review_id, place_id, tag) VALUES (r, p3, '바테이블');

    -- user3: p1 일반리뷰 + p5 일반리뷰
    INSERT INTO reviews(user_id, check_in_id, place_id, visited_at, content, taste_rating, solo_friendly_rating, created_at, updated_at)
        VALUES (u3, NULL, p1, base - interval '6 days', '1인석 많아서 부담 없음.', 4, 5, base - interval '6 days', base - interval '6 days') RETURNING id INTO r;
    INSERT INTO review_tags(review_id, place_id, tag) VALUES (r, p1, '1인석 많음');

    INSERT INTO reviews(user_id, check_in_id, place_id, visited_at, content, taste_rating, solo_friendly_rating, created_at, updated_at)
        VALUES (u3, NULL, p5, base - interval '3 days', '칸막이 자리에서 파스타. 오래 앉아 있어도 눈치 없었어요.', 5, 4, base - interval '3 days', base - interval '3 days') RETURNING id INTO r;
    INSERT INTO review_tags(review_id, place_id, tag) VALUES (r, p5, '오래 OK'), (r, p5, '칸막이');

    -- user4: p1 일반리뷰
    INSERT INTO reviews(user_id, check_in_id, place_id, visited_at, content, taste_rating, solo_friendly_rating, created_at, updated_at)
        VALUES (u4, NULL, p1, base - interval '2 days', '혼자 와도 편했어요.', 4, 4, base - interval '2 days', base - interval '2 days') RETURNING id INTO r;
    INSERT INTO review_tags(review_id, place_id, tag) VALUES (r, p1, '눈치 없음');

    RAISE NOTICE '시드 완료 — me(id=%) phone=01012345678, places p1=% p2=% p3=% p4=% p5=%', me, p1, p2, p3, p4, p5;
END $$;

-- 검증용 요약 (적재 후 출력) -------------------------------------------------
SELECT p.name AS 식당,
       count(rv.id) AS 리뷰수,
       round(avg(rv.taste_rating), 1) AS 평점,
       round(avg(rv.solo_friendly_rating), 1) AS 혼밥친화도
FROM places p LEFT JOIN reviews rv ON rv.place_id = p.id
WHERE p.source = 'SEED'
GROUP BY p.id, p.name
ORDER BY p.name;
