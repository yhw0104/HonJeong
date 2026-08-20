package com.honjeong.auth.client;

/**
 * 애플 토큰 엔드포인트 연동. 가입 시 authorizationCode를 refresh token으로 바꾸고,
 * 탈퇴 시 그 토큰을 폐기한다.
 *
 * <p>탈퇴 시 폐기는 애플 심사 지침 5.1.1(v)가 요구하는 절차다 — 계정 삭제를 제공하는 앱은
 * 애플이 발급한 토큰도 함께 무효화해야 한다.
 *
 * <p>구현체는 {@code honjeong.apple.mode}로 갈린다 — mock({@link NoopAppleTokenClient})과
 * real({@link RealAppleTokenClient}).
 */
public interface AppleTokenClient {

    /**
     * authorizationCode를 refresh token으로 교환한다.
     *
     * @param authorizationCode 앱이 애플 로그인에서 받아 넘긴 1회용 코드
     * @return refresh token. <b>실패하면 null</b> — 호출자는 로그인을 실패시키지 않는다.
     */
    String exchangeRefreshToken(String authorizationCode);

    /**
     * refresh token을 폐기한다. <b>실패해도 예외를 던지지 않는다</b> — 탈퇴를 막으면 안 된다.
     *
     * @param refreshToken 가입 때 보관해 둔 애플 refresh token(없으면 아무 것도 하지 않는다)
     */
    void revoke(String refreshToken);
}
