-- 혼밥 적합도 별점을 NULL 허용으로 바꾼다.
--
-- 왜: 혼밥 친화도(AVG(solo_friendly_rating))에 '혼자 먹어보지 않은 사람'의 점수가 섞이고 있었다.
-- 같이먹기를 하고 쓴 리뷰도, 체크인 없이 쓴 리뷰도 똑같이 한 표였다. 앞으로는 혼밥 인증 리뷰만
-- 이 값을 갖는다(불변식: solo_friendly_rating IS NOT NULL <=> check_in_id IS NOT NULL).
-- 집계 쿼리는 손대지 않는다 — AVG와 COUNT(컬럼)가 NULL을 알아서 뺀다.
ALTER TABLE reviews ALTER COLUMN solo_friendly_rating DROP NOT NULL;

-- ★CHECK 제약을 걸지 않는다.
-- 2026-08-10 이전에 쓰인 '인증 없는 리뷰'에는 값이 남아 있고(사용자가 실제로 남긴 평가라
-- 지우지 않기로 했다) 그 행들이 위 불변식을 깬다. 불변식은 애플리케이션이 신규 작성에만 강제한다.
COMMENT ON COLUMN reviews.solo_friendly_rating IS
    '혼밥 적합도 별점(1~5). NULL이면 혼밥 인증이 아닌 리뷰 — 혼밥 친화도 집계에서 자동 제외된다';
