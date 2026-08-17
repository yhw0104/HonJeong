-- 애플 탈퇴 시 토큰 폐기(revoke)에 쓸 refresh token 보관 자리.
--
-- 왜 필요한가: Apple은 회원탈퇴를 제공하는 앱이 Sign in with Apple도 쓰면 탈퇴 시 토큰을
-- revoke하도록 요구한다(App Store Review Guideline 5.1.1(v)). revoke 호출에는 애플이 발급한
-- refresh token이 필요한데, 이 값은 가입 시 authorizationCode를 교환해야만 얻을 수 있다.
-- 탈퇴하는 순간에 다시 얻으려면 사용자에게 애플 재인증을 시켜야 하고, 이미 iOS 설정에서 앱 사용을
-- 중단한 사용자는 그 재인증이 실패해 탈퇴 자체가 막힌다. 그래서 가입 때 받아 둔다.
--
-- ★이름에 apple_을 붙인 이유: 우리 자체 JWT 갱신에 쓰는 refresh_tokens 테이블이 이미 있다.
--   여기에 그냥 refresh_token을 두면 둘이 같은 것처럼 읽히는데, 실제로는 전혀 다르다 —
--   하나는 우리가 발급한 세션 토큰이고 이건 애플이 발급한 폐기용 자격증명이다.
--
-- NULL 허용: 카카오 행은 항상 NULL이고, 애플이라도 code 교환에 실패하면 NULL로 남는다
-- (교환 실패가 가입을 막지 않는다 — 그 경우 탈퇴 시 revoke를 건너뛴다).
ALTER TABLE social_accounts ADD COLUMN apple_refresh_token VARCHAR(512);

COMMENT ON COLUMN social_accounts.apple_refresh_token IS
    '애플이 발급한 refresh token — 탈퇴 시 revoke 호출에만 쓴다. 카카오 행은 NULL';

-- V12의 테이블 주석("공급자 토큰은 저장 안 함")이 위 컬럼 때문에 더는 사실이 아니라 바로잡는다.
COMMENT ON TABLE social_accounts IS
    '소셜 로그인 연동(식별용. 공급자 토큰은 애플 폐기용 refresh token만 예외적으로 보관).';
