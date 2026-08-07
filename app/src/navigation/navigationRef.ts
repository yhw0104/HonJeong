// 컴포넌트 밖(푸시 리스너)에서 화면을 이동하기 위한 전역 네비게이션 참조.
import { createNavigationContainerRef } from '@react-navigation/native';

import type { RootStackParamList } from './types';

/**
 * 훅은 컴포넌트 안에서만 쓸 수 있는데, 푸시 리스너는 앱이 꺼진 상태에서도 불릴 수 있어
 * 훅으로는 닿지 않는다. App.tsx의 NavigationContainer에 ref로 연결한다.
 */
export const navigationRef = createNavigationContainerRef<RootStackParamList>();

/** 준비된 경우에만 이동한다. 준비 전 호출은 조용히 무시된다(앱 기동 직후 짧은 구간). */
export function navigateFromPush(
  screen: keyof RootStackParamList,
  params?: Record<string, unknown>,
): void {
  if (!navigationRef.isReady()) return;
  // @ts-expect-error — screen이 런타임에 정해지는 union이라 params와 짝을 정적으로 좁힐 수 없다.
  // 화면 이름과 params의 짝은 pushTarget이 보증한다(target.ts).
  navigationRef.navigate(screen, params);
}
