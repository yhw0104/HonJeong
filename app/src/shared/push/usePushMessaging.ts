// 푸시 수신 배선(리스너 3종 + 토큰 갱신). App 루트에서 한 번만 호출한다.
//
// 이 파일은 @/shared/push(index.ts)를 import하므로 jest에서 로드할 수 없다
// (Native module NativeRNFBTurboApp is not registered). 그래서 판단 로직은 전부
// target.ts·invalidate.ts 순수 함수로 빼 두었고 여기에는 배선만 남긴다.
import React from 'react';
import { useQueryClient } from '@tanstack/react-query';
import {
  getInitialNotification,
  getMessaging,
  onMessage,
  onNotificationOpenedApp,
} from '@react-native-firebase/messaging';

import { navigateFromPush } from '@/navigation/navigationRef';
import { onPushTokenRefresh } from '.';
import { pushInvalidationKeys } from './invalidate';
import { pushTarget, type PushData } from './target';

/**
 * 세 경로를 모두 처리한다:
 *  - onMessage                앱을 보고 있을 때 (iOS는 배너를 안 띄운다 — firebase.json에서 의도적으로 비활성)
 *  - onNotificationOpenedApp  백그라운드에서 배너를 눌렀을 때
 *  - getInitialNotification   앱이 꺼진 상태에서 배너를 눌러 켜졌을 때
 */
export function usePushMessaging(): void {
  const qc = useQueryClient();

  const invalidate = React.useCallback(
    (data: PushData) => {
      pushInvalidationKeys(data.type).forEach((queryKey) => qc.invalidateQueries({ queryKey }));
    },
    [qc],
  );

  const open = React.useCallback(
    (data: PushData) => {
      invalidate(data);
      const target = pushTarget(data);
      if (target) navigateFromPush(target.screen, target.params);
    },
    [invalidate],
  );

  React.useEffect(() => {
    const messaging = getMessaging();

    // 포그라운드: 배너를 띄우지 않고 화면만 갱신한다(실시간 전략 §5의 "새로고침 신호").
    const offMessage = onMessage(messaging, async (m) => invalidate((m.data ?? {}) as PushData));

    // 백그라운드에서 배너 탭.
    const offOpened = onNotificationOpenedApp(messaging, (m) => open((m.data ?? {}) as PushData));

    // 앱이 꺼진 상태에서 배너 탭으로 켜진 경우 — 한 번만 확인한다.
    void getInitialNotification(messaging).then((m) => {
      if (m) open((m.data ?? {}) as PushData);
    });

    // FCM이 토큰을 교체하면 서버에 재등록한다.
    const offRefresh = onPushTokenRefresh();

    return () => {
      offMessage();
      offOpened();
      offRefresh();
    };
  }, [invalidate, open]);
}
