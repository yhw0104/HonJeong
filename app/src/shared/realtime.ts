import { AppState, type AppStateStatus } from 'react-native';
import { focusManager } from '@tanstack/react-query';

/**
 * 라이브(실시간) 쿼리 폴링 주기(ms).
 * "지금 누가 어디서 혼밥중인지"처럼 다른 사용자의 변화가 곧바로 보여야 하는
 * 쿼리(useStats·useMap·useNearby·useActiveDiners)에 `refetchInterval`로 사용한다.
 */
export const LIVE_REFETCH_MS = 15_000;

/**
 * RN AppState 값을 React Query "포커스됨" 여부로 변환한다.
 * 앱이 포그라운드('active')일 때만 포커스로 간주 → 백그라운드에선 폴링이 멈췄다가
 * 다시 돌아오면(focusManager.setFocused(true)) 즉시 갱신된다.
 */
export function appStateToFocused(status: AppStateStatus): boolean {
  return status === 'active';
}

/**
 * RN AppState 변화를 React Query focusManager에 연결한다.
 * 앱이 백그라운드 → 포그라운드로 돌아오면 활성 쿼리를 즉시 다시 불러온다.
 * App 루트에서 한 번 호출하고, 반환된 정리 함수를 unmount 시 호출한다.
 */
export function setupRealtimeFocus(): () => void {
  const sub = AppState.addEventListener('change', (status) => {
    focusManager.setFocused(appStateToFocused(status));
  });
  return () => sub.remove();
}
