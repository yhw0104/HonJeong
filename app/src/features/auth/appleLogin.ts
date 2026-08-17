// 애플 로그인 SDK 호출을 이 파일에만 둔다 — 화면은 SDK를 직접 모르게 한다(kakaoLogin.ts와 같은 규칙).
import * as AppleAuthentication from 'expo-apple-authentication';

export type AppleCredential = {
  /** 서버가 검증할 애플 ID 토큰. */
  identityToken: string;
  /** 탈퇴 시 토큰 폐기에 쓸 단기 인가 코드. 없어도 로그인은 된다. */
  authorizationCode: string | null;
};

/** 이 기기에서 애플 로그인을 쓸 수 있는지(iOS 13+). 버튼 노출 여부를 정하는 데 쓴다. */
export async function isAppleLoginAvailable(): Promise<boolean> {
  try {
    return await AppleAuthentication.isAvailableAsync();
  } catch {
    // 현재 SDK에서는 여기 오지 않는다 — 미지원 플랫폼(안드로이드·웹)이나 네이티브 모듈이 링크되지
    // 않은 dev 빌드에서 SDK는 던지지 않고 false를 돌려준다(ExpoAppleAuthentication.ts의 폴백 객체).
    // 그래도 감싸 둔다: 이건 "쓸 수 있나?"를 묻는 질문일 뿐이라, 답을 못 얻었다고 화면을 깨뜨리는
    // 것보다 버튼을 감추는 쪽이 언제나 낫다.
    return false;
  }
}

/**
 * 애플 로그인 → 자격증명.
 * - 성공: { identityToken, authorizationCode }
 * - 사용자가 취소: null (에러 알림을 띄우면 안 되는 정상 이탈)
 * - 그 외 실패: throw
 *
 * ★ requestedScopes를 비워 둔다 — 이메일·이름을 요청하지 않는다. 우리는 둘 다 저장하지 않기로 했고
 *   그 사실을 개인정보 처리방침에 적었다(2026-08-14 게시). 스코프를 비우면 애플 동의 화면에
 *   이메일 선택지 자체가 뜨지 않으므로 Private Relay를 다룰 일도 없다. 서버가 쓰는 sub는
 *   스코프와 무관하게 identityToken에 들어 있다.
 *   같은 이유로 응답의 email·fullName은 읽지도 않는다 — 최초 로그인 응답에 값이 실려 오더라도
 *   지역변수로도 꺼내지 않고 여기서 버린다. 안 가져온 값은 샐 수 없다.
 */
export async function loginWithApple(): Promise<AppleCredential | null> {
  try {
    const credential = await AppleAuthentication.signInAsync({ requestedScopes: [] });
    if (!credential.identityToken) {
      // ★현재 SDK에서는 도달하지 않는 방어선이다 — signInAsync가 identityToken·authorizationCode·user
      // 중 하나라도 비어 있으면 스스로 ERR_REQUEST_FAILED로 던진다(AppleAuthentication.ts).
      // 실제로 관찰한 실패 경로가 아니라, 그 검사가 사라질 때를 대비한 보험이다.
      // 그래도 남겨 둔다: 토큰 없는 자격증명을 성공으로 위장해 서버로 보내는 것보단 낫다.
      throw new Error('애플에서 ID 토큰을 받지 못했습니다.');
    }
    return {
      identityToken: credential.identityToken,
      // 같은 이유로 authorizationCode도 실제로는 비어 올 수 없다. 타입이 string | null이라 좁혀 줄 뿐이다.
      authorizationCode: credential.authorizationCode ?? null,
    };
  } catch (e) {
    if (isUserCancelled(e)) return null;
    throw e;
  }
}

/**
 * 사용자가 애플 시트에서 취소한 경우를 판별한다.
 * expo-apple-authentication은 취소를 `code`로 알려준다 — 네이티브 RequestCanceledException이
 * ERR_REQUEST_CANCELED로 넘어온다.
 * 정확히 비교하지 않고 'cancel' 포함으로 보는 이유: 철자(canceled/cancelled)와 대소문자가
 * 플랫폼·버전에 따라 갈릴 수 있는데, 이 모듈의 다른 예외 중 'cancel'을 품은 이름이 하나도 없어
 * 오탐 위험이 없기 때문이다(InvalidScope·InvalidOperation·InvalidResponse·RequestNotHandled·
 * RequestFailed·RequestNotInteractive·RequestUnknown·RequestMatchedExcludedCredential, 그리고
 * JS 쪽의 ERR_REQUEST_FAILED·ERR_UNAVAILABLE). 클래스 이름이 바뀌어도 이 판정은 살아남는다.
 *
 * code가 아예 없는 예외적인 경우에만 메시지 매칭을 폴백으로 쓴다.
 * ★폴백의 트레이드오프: code 없는 실패의 메시지에 우연히 'cancel'이 들어 있으면 취소로 오인해
 *   null을 돌려주고, 그러면 사용자에겐 버튼이 아무 반응도 하지 않은 것처럼 보인다(에러 알림도 없다).
 *   kakaoLogin.ts가 똑같은 판정을 쓰고 있어 일관성을 위해 그대로 둔다 — 바꾼다면 두 파일을 같이 바꾼다.
 */
function isUserCancelled(e: unknown): boolean {
  const code = e && typeof e === 'object' && 'code' in e ? (e as { code?: unknown }).code : undefined;
  if (typeof code === 'string' && code.length > 0) {
    return code.toLowerCase().includes('cancel');
  }
  const message = e instanceof Error ? e.message : String(e);
  return /cancel/i.test(message);
}
