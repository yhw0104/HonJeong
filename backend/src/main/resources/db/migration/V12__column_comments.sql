-- ============================================================================
-- V12__column_comments.sql — 여태 만든 모든 테이블·컬럼에 설명(COMMENT) 부여.
-- 구조/데이터 변경 없음. psql \d+ · TablePlus/DBeaver 등에서 컬럼 설명으로 노출된다.
-- 스키마 자체의 source of truth는 각 CREATE TABLE(V1~V11)이고, 여기선 문서화만 한다.
-- ============================================================================

-- A. 인증·계정 ---------------------------------------------------------------

COMMENT ON TABLE  users IS '회원. 온보딩(status=PENDING) 중엔 phone/nickname NULL 허용, /auth/complete에서 채워지고 ACTIVE 전환.';
COMMENT ON COLUMN users.id                 IS '회원 PK';
COMMENT ON COLUMN users.phone              IS '휴대폰 번호(인증완료)·1차 식별자. UNIQUE';
COMMENT ON COLUMN users.email              IS 'OAuth 제공 이메일(선택)';
COMMENT ON COLUMN users.nickname           IS '표시 이름(중복 불가). UNIQUE';
COMMENT ON COLUMN users.profile_image_url  IS '프로필 이미지 URL';
COMMENT ON COLUMN users.gender             IS '성별: MALE|FEMALE|NONE';
COMMENT ON COLUMN users.age_group          IS '연령대: 10s|20s|30s|40s|50+';
COMMENT ON COLUMN users.introduction       IS '자기소개(최대 150자)';
COMMENT ON COLUMN users.region             IS '동네(시군구·동)';
COMMENT ON COLUMN users.region_lat         IS '동네 중심 위도';
COMMENT ON COLUMN users.region_lng         IS '동네 중심 경도';
COMMENT ON COLUMN users.dining_style       IS '식사 성향: TALK(대화)|QUIET(조용히)';
COMMENT ON COLUMN users.allow_meal_request IS '같이먹기 수신 opt-in(true면 신청 받음)';
COMMENT ON COLUMN users.status             IS '계정 상태: PENDING|ACTIVE|SUSPENDED|WITHDRAWN';
COMMENT ON COLUMN users.created_at         IS '생성 시각';
COMMENT ON COLUMN users.updated_at         IS '수정 시각';

COMMENT ON TABLE  refresh_tokens IS 'refresh 토큰(DB 저장 + 회전). 원문 대신 해시 저장, 로그아웃/탈취 시 revoke.';
COMMENT ON COLUMN refresh_tokens.id         IS 'PK';
COMMENT ON COLUMN refresh_tokens.user_id    IS '소유 회원 FK → users.id';
COMMENT ON COLUMN refresh_tokens.token_hash IS 'refresh 토큰 해시(원문 미저장). UNIQUE';
COMMENT ON COLUMN refresh_tokens.expires_at IS '토큰 만료 시각';
COMMENT ON COLUMN refresh_tokens.revoked    IS '폐기 여부(로그아웃/회전 시 true)';
COMMENT ON COLUMN refresh_tokens.created_at IS '생성 시각';
COMMENT ON COLUMN refresh_tokens.updated_at IS '수정 시각';

COMMENT ON TABLE  social_accounts IS '소셜 로그인 연동(공급자 토큰은 저장 안 함, 식별만).';
COMMENT ON COLUMN social_accounts.id               IS 'PK';
COMMENT ON COLUMN social_accounts.user_id          IS '연동 회원 FK → users.id';
COMMENT ON COLUMN social_accounts.provider         IS '공급자: KAKAO|APPLE';
COMMENT ON COLUMN social_accounts.provider_user_id IS '공급자측 고유 sub';
COMMENT ON COLUMN social_accounts.email            IS '공급자 제공 이메일(선택)';
COMMENT ON COLUMN social_accounts.created_at       IS '생성 시각';
COMMENT ON COLUMN social_accounts.updated_at       IS '수정 시각';

COMMENT ON TABLE  phone_verifications IS '휴대폰 인증 발송·검증(회원 무관 발송도 있어 phone 기준).';
COMMENT ON COLUMN phone_verifications.id         IS 'PK';
COMMENT ON COLUMN phone_verifications.phone      IS '수신 휴대폰 번호';
COMMENT ON COLUMN phone_verifications.code       IS '인증 코드(해시 저장 권장)';
COMMENT ON COLUMN phone_verifications.expires_at IS '코드 만료 시각';
COMMENT ON COLUMN phone_verifications.verified   IS '검증 완료 여부';
COMMENT ON COLUMN phone_verifications.attempts   IS '검증 시도 횟수';
COMMENT ON COLUMN phone_verifications.created_at IS '생성(발송) 시각';

COMMENT ON TABLE  terms_agreements IS '약관 동의(사용자당 1행).';
COMMENT ON COLUMN terms_agreements.id        IS 'PK';
COMMENT ON COLUMN terms_agreements.user_id   IS '회원 FK → users.id. UNIQUE(사용자당 1행)';
COMMENT ON COLUMN terms_agreements.service   IS '서비스 이용약관 동의(필수)';
COMMENT ON COLUMN terms_agreements.privacy   IS '개인정보 처리방침 동의(필수)';
COMMENT ON COLUMN terms_agreements.location  IS '위치기반 서비스 동의(필수)';
COMMENT ON COLUMN terms_agreements.marketing IS '마케팅 수신 동의(선택)';
COMMENT ON COLUMN terms_agreements.agreed_at IS '동의 시각';

-- B. 장소 -------------------------------------------------------------------

COMMENT ON TABLE  places IS '식당 장소. 출처는 공공데이터 마스터 적재(source/source_id), 카카오는 지도 렌더링만. 좌표 필수.';
COMMENT ON COLUMN places.id              IS '장소 PK';
-- external_id·homepage_url은 V3에서 삭제됨(공공데이터 전환) → 주석 대상 아님
COMMENT ON COLUMN places.name            IS '식당 이름';
COMMENT ON COLUMN places.address         IS '지번 주소';
COMMENT ON COLUMN places.latitude        IS '위도(지도·반경 검색)';
COMMENT ON COLUMN places.longitude       IS '경도(지도·반경 검색)';
COMMENT ON COLUMN places.category        IS '카테고리(음식 종류 등)';
COMMENT ON COLUMN places.phone           IS '전화번호';
COMMENT ON COLUMN places.source          IS '데이터 출처(기본 PUBLIC_DATA)';
COMMENT ON COLUMN places.source_id       IS '출처측 식별자. (source, source_id) UNIQUE';
COMMENT ON COLUMN places.road_address    IS '도로명 주소';
COMMENT ON COLUMN places.business_status IS '영업 상태(영업/폐업 등)';
COMMENT ON COLUMN places.created_at      IS '생성 시각';
COMMENT ON COLUMN places.updated_at      IS '수정 시각';

-- C. 체크인·같이먹기 --------------------------------------------------------

COMMENT ON TABLE  check_ins IS '혼밥 체크인(핵심 데이터). 사용자당 현재 활동(ACTIVE·TOGETHER)은 부분 유니크 인덱스로 최대 1개.';
COMMENT ON COLUMN check_ins.id              IS '체크인 PK';
COMMENT ON COLUMN check_ins.user_id         IS '체크인한 회원 FK → users.id';
COMMENT ON COLUMN check_ins.place_id        IS '체크인 장소 FK → places.id';
COMMENT ON COLUMN check_ins.status          IS '상태: ACTIVE(혼밥중)|TOGETHER(같이먹는중)|ENDED(종료)|CANCELLED(취소·통계제외)';
COMMENT ON COLUMN check_ins.started_at      IS '혼밥 시작 시각(ACTIVE TTL 기준)';
COMMENT ON COLUMN check_ins.ended_at        IS '종료 시각(ENDED 전까지 NULL)';
COMMENT ON COLUMN check_ins.created_at      IS '생성 시각';
COMMENT ON COLUMN check_ins.matched_at      IS '같이먹기 매칭 시각(TOGETHER TTL 기준). 매칭 안 됐으면 NULL';
COMMENT ON COLUMN check_ins.meal_request_id IS '매칭된 같이먹기 신청 FK → meal_requests.id(같은 매칭의 양쪽 체크인이 공유). 비매칭이면 NULL';

COMMENT ON TABLE  meal_requests IS '같이먹기 신청. 수신자는 to_check_in_id → check_ins.user_id로 식별. (from_user_id, to_check_in_id) 중복 방지.';
COMMENT ON COLUMN meal_requests.id             IS '신청 PK';
COMMENT ON COLUMN meal_requests.from_user_id   IS '신청자 FK → users.id';
COMMENT ON COLUMN meal_requests.to_check_in_id IS '대상 체크인 FK → check_ins.id(수신자 = 그 체크인 주인)';
COMMENT ON COLUMN meal_requests.place_id       IS '신청 발생 장소 FK → places.id(대상 체크인 장소 역정규화)';
COMMENT ON COLUMN meal_requests.message        IS '인사 한마디(선택, 최대 200자)';
COMMENT ON COLUMN meal_requests.status         IS '상태: PENDING|ACCEPTED|DECLINED';
COMMENT ON COLUMN meal_requests.created_at     IS '신청 시각';
COMMENT ON COLUMN meal_requests.responded_at   IS '수락/거절 시각(PENDING 동안 NULL)';

-- D. 선호 음식 --------------------------------------------------------------

COMMENT ON TABLE  user_food_preferences IS '선호 음식(사용자당 1행, 최대 3개를 고정 컬럼으로 보관).';
COMMENT ON COLUMN user_food_preferences.id         IS 'PK';
COMMENT ON COLUMN user_food_preferences.user_id    IS '회원 FK → users.id. UNIQUE(사용자당 1행)';
COMMENT ON COLUMN user_food_preferences.food1      IS '선호 음식 1(선택)';
COMMENT ON COLUMN user_food_preferences.food2      IS '선호 음식 2(선택)';
COMMENT ON COLUMN user_food_preferences.food3      IS '선호 음식 3(선택)';
COMMENT ON COLUMN user_food_preferences.created_at IS '생성 시각';
COMMENT ON COLUMN user_food_preferences.updated_at IS '수정 시각';

-- E. 리뷰(혼밥일기) ---------------------------------------------------------

COMMENT ON TABLE  reviews IS '리뷰=혼밥일기. 별점 2종 필수, 인증 체크인(check_in_id)은 선택.';
COMMENT ON COLUMN reviews.id                   IS '리뷰 PK';
COMMENT ON COLUMN reviews.user_id              IS '작성자 FK → users.id';
COMMENT ON COLUMN reviews.check_in_id          IS '인증 연결 체크인 FK → check_ins.id(있으면 방문 인증, NULL 허용)';
COMMENT ON COLUMN reviews.place_id             IS '식당 FK → places.id';
COMMENT ON COLUMN reviews.visited_at           IS '방문 시각';
COMMENT ON COLUMN reviews.content              IS '리뷰 본문';
COMMENT ON COLUMN reviews.taste_rating         IS '맛 별점(1~5)';
COMMENT ON COLUMN reviews.solo_friendly_rating IS '혼밥 친화 별점(1~5)';
COMMENT ON COLUMN reviews.created_at           IS '생성 시각';
COMMENT ON COLUMN reviews.updated_at           IS '수정 시각';

COMMENT ON TABLE  review_tags IS '리뷰 친화 태그. place_id 역정규화로 식당별 태그 집계.';
COMMENT ON COLUMN review_tags.id        IS 'PK';
COMMENT ON COLUMN review_tags.review_id IS '리뷰 FK → reviews.id';
COMMENT ON COLUMN review_tags.place_id  IS '식당 FK → places.id(reviews에서 역정규화·불변)';
COMMENT ON COLUMN review_tags.tag       IS '친화 태그 값';

COMMENT ON TABLE  review_photos IS '리뷰 사진(review 1:N). 식당 사진탭 갤러리의 출처. 리뷰 삭제 시 CASCADE.';
COMMENT ON COLUMN review_photos.id         IS 'PK';
COMMENT ON COLUMN review_photos.review_id  IS '리뷰 FK → reviews.id (ON DELETE CASCADE)';
COMMENT ON COLUMN review_photos.image_url  IS '사진 URL';
COMMENT ON COLUMN review_photos.sort_order IS '표시 순서';
COMMENT ON COLUMN review_photos.created_at IS '생성 시각';

-- F. 즐겨찾기 ---------------------------------------------------------------

COMMENT ON TABLE  favorite_groups IS '즐겨찾기 그룹(사용자별). 가입 시 기본 그룹 1개 백필.';
COMMENT ON COLUMN favorite_groups.id         IS 'PK';
COMMENT ON COLUMN favorite_groups.user_id    IS '소유 회원 FK → users.id (ON DELETE CASCADE)';
COMMENT ON COLUMN favorite_groups.name       IS '그룹 이름';
COMMENT ON COLUMN favorite_groups.note       IS '그룹 메모(선택)';
COMMENT ON COLUMN favorite_groups.color      IS '그룹 색상(HEX, 기본 #FF5A1F)';
COMMENT ON COLUMN favorite_groups.is_default IS '기본 그룹 여부';
COMMENT ON COLUMN favorite_groups.created_at IS '생성 시각';
COMMENT ON COLUMN favorite_groups.updated_at IS '수정 시각';

COMMENT ON TABLE  favorites IS '즐겨찾기 항목(그룹 내 장소). (group_id, place_id) 중복 방지.';
COMMENT ON COLUMN favorites.id         IS 'PK';
COMMENT ON COLUMN favorites.group_id   IS '그룹 FK → favorite_groups.id (ON DELETE CASCADE)';
COMMENT ON COLUMN favorites.place_id   IS '장소 FK → places.id (ON DELETE CASCADE)';
COMMENT ON COLUMN favorites.created_at IS '추가 시각';

-- G. 메이트(지속 관계) ------------------------------------------------------

COMMENT ON TABLE  mate_requests IS '메이트 신청. 진행 중(PENDING) 동일 쌍만 유일 강제 → 거절/취소 후 재신청 허용.';
COMMENT ON COLUMN mate_requests.id           IS 'PK';
COMMENT ON COLUMN mate_requests.from_user_id IS '신청자 FK → users.id (ON DELETE CASCADE)';
COMMENT ON COLUMN mate_requests.to_user_id   IS '대상 FK → users.id (ON DELETE CASCADE)';
COMMENT ON COLUMN mate_requests.status       IS '상태: PENDING|ACCEPTED|DECLINED';
COMMENT ON COLUMN mate_requests.created_at   IS '신청 시각';
COMMENT ON COLUMN mate_requests.responded_at IS '수락/거절 시각(PENDING 동안 NULL)';

COMMENT ON TABLE  mates IS '메이트 관계. 수락 시 양방향 2행(a→b, b→a) 저장 → 내 메이트 조회는 WHERE user_id=:me로 단순.';
COMMENT ON COLUMN mates.id           IS 'PK';
COMMENT ON COLUMN mates.user_id      IS '기준 회원 FK → users.id (ON DELETE CASCADE)';
COMMENT ON COLUMN mates.mate_user_id IS '메이트 상대 FK → users.id (ON DELETE CASCADE). (user_id, mate_user_id) UNIQUE';
COMMENT ON COLUMN mates.created_at   IS '메이트 성립 시각';
