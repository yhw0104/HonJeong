// 푸시 권한 사전 안내를 띄울지 판단하는 로직 + '안내를 봤다' 플래그 저장.
//
// 이 파일은 shared/push/index.ts를 import하지 않는다 — index.ts는 @react-native-firebase를
// import 시점에 조회해서 jest에서 즉사한다(Native module NativeRNFBTurboApp is not registered).
// 순수 판단 로직을 여기 두면 테스트가 붙는다.
import * as SecureStore from 'expo-secure-store';

// SecureStore 키는 영숫자·.·-·_ 만 허용된다(Expo SDK 56 문서).
const SEEN_KEY = 'honjeong.pushPromptSeen';

/**
 * 권한 안내 화면을 띄울지 판단한다.
 *
 * 이미 권한이 있으면 안내가 무의미하고, 이미 안내를 봤다면 다시 띄우지 않는다
 * (거절한 사람에게 매번 들이대지 않는다 — 다시 켜는 길은 알림 설정 화면에 있다).
 */
export function shouldPromptPush({ seen, granted }: { seen: boolean; granted: boolean }): boolean {
  if (granted) return false;
  return !seen;
}

/** 안내를 본 적 있는지. secure-store 접근이 실패하면 "본 것"으로 취급해 반복 노출을 막는다. */
export async function hasSeenPushPrompt(): Promise<boolean> {
  try {
    return (await SecureStore.getItemAsync(SEEN_KEY)) === '1';
  } catch {
    return true;
  }
}

/** 안내를 봤다고 기록한다(수락·거절 무관 — 화면을 띄운 사실 자체를 남긴다). */
export async function markPushPromptSeen(): Promise<void> {
  try {
    await SecureStore.setItemAsync(SEEN_KEY, '1');
  } catch {
    // 실패해도 흐름을 막지 않는다. 다음에 한 번 더 뜨는 정도의 비용이다.
  }
}
