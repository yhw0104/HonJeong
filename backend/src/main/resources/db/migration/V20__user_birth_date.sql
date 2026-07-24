-- 연령 저장을 '연령대 문자열'에서 '생년월일'로 전환. 표시 연령대는 서버가 생년월일로 파생.
-- 사전출시라 기존 age_group 값은 이관하지 않는다(테스트 데이터 초기화 전제).
ALTER TABLE users DROP COLUMN age_group;
ALTER TABLE users ADD COLUMN birth_date DATE;
COMMENT ON COLUMN users.birth_date IS '생년월일(온보딩 고정). 표시 연령대는 서버가 연 나이로 파생하며 응답엔 미노출';
