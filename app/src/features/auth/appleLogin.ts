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
    // 이건 "쓸 수 있나?"를 묻는 질문일 뿐이다. 답을 못 얻었다고 화면을 깨뜨리는 대신
    // 못 쓰는 것으로 보고 버튼을 감춘다(안드로이드·구형 iOS·네이티브 모듈 미링크 dev 빌드).
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
      // 원인을 그대로 드러낸다 — 조용히 성공으로 위장하지 않는다.
      throw new Error('애플에서 ID 토큰을 받지 못했습니다.');
    }
    return {
      identityToken: credential.identityToken,
      authorizationCode: credential.authorizationCode ?? null,
    };
  } catch (e) {
    if (isUserCancelled(e)) return null;
    throw e;
  }
}

/**
 * 사용자가 애플 시트에서 취소한 경우를 판별한다.
 * expo-apple-authentication은 취소를 `code`로 알려준다 — 문서상 값은 ERR_REQUEST_CANCELED다.
 * 정확히 비교하지 않고 'cancel' 포함으로 보는 이유: 철자(canceled/cancelled)와 대소문자가
 * 플랫폼·버전에 따라 갈릴 수 있는데, 애플 모듈의 다른 에러 코드(ERR_REQUEST_FAILED,
 * ERR_REQUEST_NOT_HANDLED, ERR_APPLE_AUTHENTICATION_UNAVAILABLE 등) 중 'cancel'을 품은 것은 없어
 * 오탐 위험이 없기 때문이다. code가 아예 없는 예외적인 경우에만 메시지 매칭을 폴백으로 쓴다.
 */
function isUserCancelled(e: unknown): boolean {
  const code = e && typeof e === 'object' && 'code' in e ? (e as { code?: unknown }).code : undefined;
  if (typeof code === 'string' && code.length > 0) {
    return code.toLowerCase().includes('cancel');
  }
  const message = e instanceof Error ? e.message : String(e);
  return /cancel/i.test(message);
}
