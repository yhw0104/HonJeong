-- ============================================================================
-- V7__reviews_unique_checkin.sql — 한 방문(체크인)에 리뷰 1건만.
-- 인증 리뷰(check_in_id 존재)에만 적용하는 부분 유니크 인덱스.
-- 일반 리뷰(check_in_id NULL)는 다건 허용(부분 인덱스라 NULL 행은 제약 대상 아님).
--
-- 인덱스 생성 전, 기존에 같은 check_in_id로 2건+ 쌓인 중복(출시 전 버그로 생긴 더미)을
-- check_in_id별 가장 최근(MAX id) 1건만 남기고 정리한다. 운영 DB엔 해당 데이터가 없어 no-op.
-- ============================================================================

-- 1) 중복 리뷰의 태그부터 제거(FK)
DELETE FROM review_tags WHERE review_id IN (
    SELECT r.id FROM reviews r
    WHERE r.check_in_id IS NOT NULL
      AND r.id < (SELECT MAX(r2.id) FROM reviews r2 WHERE r2.check_in_id = r.check_in_id)
);

-- 2) check_in_id별 최신 1건만 남기고 중복 리뷰 제거
DELETE FROM reviews r
WHERE r.check_in_id IS NOT NULL
  AND r.id < (SELECT MAX(r2.id) FROM reviews r2 WHERE r2.check_in_id = r.check_in_id);

-- 3) 부분 유니크 인덱스
CREATE UNIQUE INDEX uq_reviews_check_in ON reviews(check_in_id) WHERE check_in_id IS NOT NULL;
