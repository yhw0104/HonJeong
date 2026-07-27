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
  initializeKakaoSDK(KAKAO_NATIVE_APP_KEY);
}

/**
 * 카카오 로그인 → OIDC ID 토큰.
 * - 성공: idToken 문자열
 * - 사용자가 취소: null (에러 알림을 띄우면 안 되는 정상 이탈)
 * - 그 외 실패: throw
 */
export async function loginWithKakao(): Promise<string | null> {
  try {
    const token = await login();
    if (!token.idToken) {
      // 콘솔에서 OpenID Connect를 켜지 않으면 여기로 온다 — 원인을 그대로 드러낸다.
      throw new Error('카카오에서 ID 토큰을 받지 못했습니다. 콘솔의 OpenID Connect 활성화를 확인하세요.');
    }
    return token.idToken;
  } catch (e) {
    if (isUserCancelled(e)) return null;
    throw e;
  }
}

/** 사용자가 카카오 화면에서 취소한 경우를 판별한다(플랫폼마다 메시지가 달라 넓게 본다). */
function isUserCancelled(e: unknown): boolean {
  const message = e instanceof Error ? e.message : String(e);
  return /cancel/i.test(message);
}
