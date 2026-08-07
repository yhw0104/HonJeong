// RNFirebase 격리 계층. 앱의 나머지 코드는 이 모듈만 알고 @react-native-firebase를 직접 부르지 않는다
// (07-27 kakaoLogin.ts와 같은 방식 — 전달 경로를 바꿔도 여기만 고친다).
//
// v26의 messaging은 modular 전용이다. 네임스페이스 API(messaging().requestPermission())는
// 더 이상 없고(default export 자체가 없다) 아래처럼 인스턴스를 첫 인자로 넘기는 함수만 있다.
//
// requestPermission/hasPermission은 v26에서 @deprecated로 표시돼 있다(권한은 expo-notifications나
// react-native-permissions로 옮기라는 안내). 아직 동작하고 이 앱은 두 패키지를 쓰지 않으므로 그대로 쓴다.
// 다음 메이저에서 제거되면 그때 이 파일만 갈아끼우면 된다 — 그러라고 격리해 둔 것이다.
import { Platform } from 'react-native';
import {
  AuthorizationStatus,
  getMessaging,
  getToken,
  hasPermission,
  onTokenRefresh,
  requestPermission,
} from '@react-native-firebase/messaging';

import { deleteDeviceToken, registerDeviceToken, type PushPlatform } from './api';

const platform = (): PushPlatform => (Platform.OS === 'android' ? 'ANDROID' : 'IOS');

// PROVISIONAL은 배너 없이 알림함에만 조용히 쌓이는 상태다. 발송 대상은 되므로 허용으로 친다.
function granted(status: number): boolean {
  return status === AuthorizationStatus.AUTHORIZED || status === AuthorizationStatus.PROVISIONAL;
}

/** OS 권한 팝업을 띄운다. iOS는 한 번 거부되면 다시 띄울 수 없으므로 호출 전에 안내 화면을 거친다. */
export async function requestPushPermission(): Promise<boolean> {
  try {
    return granted(await requestPermission(getMessaging()));
  } catch {
    return false;
  }
}

/** 팝업 없이 현재 권한 상태만 확인한다. */
export async function hasPushPermission(): Promise<boolean> {
  try {
    return granted(await hasPermission(getMessaging()));
  } catch {
    return false;
  }
}

/** FCM 등록 토큰. 권한이 없거나 아직 APNs 등록 전이면 null. */
export async function getPushToken(): Promise<string | null> {
  try {
    return (await getToken(getMessaging())) || null;
  } catch {
    return null;
  }
}

/**
 * 권한이 있으면 토큰을 받아 서버에 등록한다. 권한이 없으면 아무것도 하지 않는다 —
 * 여기서 팝업을 띄우면 안 된다. iOS는 한 번 거부당하면 앱에서 다시 못 물어보므로
 * 그 기회는 안내 화면에서만 쓴다.
 * 실패해도 예외를 던지지 않는다 — 로그인·앱 시작을 막을 이유가 아니다.
 */
export async function registerPushToken(): Promise<void> {
  try {
    if (!(await hasPushPermission())) return;
    const token = await getPushToken();
    if (!token) return;
    await registerDeviceToken(token, platform());
  } catch {
    // 조용히 넘어간다. 다음 앱 시작 때 다시 시도된다.
  }
}

/**
 * 이 기기 토큰을 서버에서 지운다(로그아웃).
 * 실패해도 예외를 던지지 않는다 — 로그아웃이 막히면 안 된다.
 */
export async function unregisterPushToken(): Promise<void> {
  try {
    const token = await getPushToken();
    if (!token) return;
    await deleteDeviceToken(token);
  } catch {
    // 세션이 이미 만료된 경우가 대부분이라 정상이다.
    // 서버의 등록 UPSERT(주인 갱신)가 최종 방어선이 된다.
  }
}

/** FCM이 토큰을 교체할 때 재등록한다. 반환값은 구독 해제 함수. */
export function onPushTokenRefresh(): () => void {
  return onTokenRefresh(getMessaging(), async (token) => {
    try {
      await registerDeviceToken(token, platform());
    } catch {
      // 조용히 넘어간다.
    }
  });
}
