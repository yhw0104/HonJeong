// 카카오 SDK 호출을 이 파일에만 둔다 — 화면은 SDK를 직접 모르게 하고,
// 나중에 웹 로그인 방식으로 갈아타도 여기만 바뀌게 한다.
import { initializeKakaoSDK } from '@react-native-kakao/core';
import { login } from '@react-native-kakao/user';

export const KAKAO_NATIVE_APP_KEY = process.env.EXPO_PUBLIC_KAKAO_NATIVE_APP_KEY;

/** 앱 시작 시 1회. 키가 없으면 앱을 죽이지 않고 경고만 남긴다(로그인만 불가). */
export function initKakao(): void {
  if (!KAKAO_NATIVE_APP_KEY) {
    console.warn('[kakao] 네이티브 앱 키가 없어 카카오 로그인을 초기화하지 못했습니다.');
    return;
  }
  // initializeKakaoSDK는 Promise<void>를 반환한다. 네이티브 모듈이 아직 링크되지 않은 상태(예: dev build 재생성 전)에서는
  // 이 프로미스가 reject되는데, await 없이 그대로 두면 매 앱 부팅마다 unhandled rejection이 발생한다.
  // 브리프 의도대로 "실패해도 앱을 죽이지 않고 경고만" 남기도록 catch로 흡수한다.
  initializeKakaoSDK(KAKAO_NATIVE_APP_KEY).catch((e: unknown) => {
    console.warn('[kakao] 카카오 SDK 초기화에 실패했습니다.', e);
  });
}

/**
 * 카카오 로그인 요청마다 새로 만드는 nonce.
 * - 용도: 카카오 SDK에 nonce를 실어 보내면 그것이 "OIDC 요청"이라는 신호가 되어 scope=openid가 실리고
 *   id_token이 발급된다(nonce 없이 로그인하면 카카오가 OIDC가 아닌 일반 로그인으로 취급해 idToken을 주지 않는다).
 * - 정직한 한계: 우리 서버는 무상태이고 이 nonce 값을 저장하지도, id_token의 nonce claim과 대조 검증하지도 않는다.
 *   즉 이 값은 재전송 공격을 서버 측에서 막아주는 보안 장치가 아니라, 순수히 카카오에게 OIDC 플로우를 타게 만드는
 *   트리거 값일 뿐이다. 매 로그인 시도마다 새로 만들어 재사용하지 않는 정도만 지킨다.
 * - expo-crypto 등 새 패키지 없이(네이티브 재빌드 회피) Math.random 두 번 + 타임스탬프 조합으로 충분히
 *   예측하기 어려운 문자열을 만든다.
 */
function createLoginNonce(): string {
  const random = Math.random().toString(36).slice(2) + Math.random().toString(36).slice(2);
  return `${Date.now().toString(36)}${random}`;
}

/**
 * 카카오 로그인 → OIDC ID 토큰.
 * - 성공: idToken 문자열
 * - 사용자가 취소: null (에러 알림을 띄우면 안 되는 정상 이탈)
 * - 그 외 실패: throw
 */
export async function loginWithKakao(): Promise<string | null> {
  try {
    // 주의: scopes 옵션은 넘기지 않는다. scopes가 비어있지 않으면 네이티브 구현
    // (iOS: RNCKakaoUserManager.swift, Android: RNCKakaoUserModule.kt)이
    // UserApi.shared.loginWithKakaoAccount(scopes:nonce:)로 분기하는데, 이 API는
    // "추가 항목 동의 받기"(incremental authorization) 전용이라 이미 발급된 액세스
    // 토큰을 전제로 한다. 최초 로그인에는 쓸 수 없다 — 실기 확인 결과 로그인 화면 자체가
    // 뜨지 않고 즉시 `Error: required parameter access_token not presented.`로 실패했다.
    const token = await login({ nonce: createLoginNonce() });
    if (!token.idToken) {
      // 콘솔에서 OpenID Connect를 켜지 않으면 여기로 온다 — 원인을 그대로 드러낸다.
      // 자격증명(accessToken 등)은 절대 찍지 않고, 어떤 필드가 왔는지(키 이름)만 남겨 원인 판별을 돕는다.
      console.warn('[kakao] idToken이 없는 응답을 받았습니다. 응답 필드 목록:', Object.keys(token));
      throw new Error('카카오에서 ID 토큰을 받지 못했습니다. 콘솔의 OpenID Connect 활성화를 확인하세요.');
    }
    return token.idToken;
  } catch (e) {
    if (isUserCancelled(e)) return null;
    throw e;
  }
}

/**
 * 사용자가 카카오 화면에서 취소한 경우를 판별한다.
 * RN 브릿지가 reject(code, message, ...) 형태로 던지기 때문에, 던져진 Error에는 카카오 SDK가 실어준
 * 구조화된 원인이 `code`에 담겨 있다(안드로이드: RNCKakaoUtil.kt에서 `e.reason.name` → 취소 시 "Cancelled",
 * iOS: RNCkakaoUtil.swift에서 `"\(reason)"` 보간 → 취소 시 소문자 "cancelled"일 수 있음).
 * 플랫폼마다 대소문자가 다르므로 대소문자 무시로 비교하고, code가 없는 예외적인 경우에만
 * 기존의 메시지 문자열 매칭을 폴백으로 사용한다.
 */
function isUserCancelled(e: unknown): boolean {
  const code = e && typeof e === 'object' && 'code' in e ? (e as { code?: unknown }).code : undefined;
  if (typeof code === 'string' && code.length > 0) {
    return code.toLowerCase() === 'cancelled' || code.toLowerCase() === 'canceled';
  }
  const message = e instanceof Error ? e.message : String(e);
  return /cancel/i.test(message);
}
