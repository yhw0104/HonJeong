-- 약관 동의에 '만 14세 이상 확인'(필수) 추가.
-- 만 14세 미만은 법정대리인 동의가 필요하므로 가입 시 필수 확인 항목으로 받는다.
-- 기존 동의 행은 age=false로 채운다(과거 가입자는 별도 재확인 대상).
ALTER TABLE terms_agreements ADD COLUMN age BOOLEAN NOT NULL DEFAULT FALSE;
COMMENT ON COLUMN terms_agreements.age IS '만 14세 이상 확인(필수)';
