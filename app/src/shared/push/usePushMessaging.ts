// 푸시 수신 배선(리스너 3종 + 토큰 갱신). App 루트에서 한 번만 호출한다.
//
// 이 파일은 @/shared/push(index.ts)를 import하므로 jest에서 로드할 수 없다
// (Native module NativeRNFBTurboApp is not registered). 그래서 판단 로직은 전부
// target.ts·invalidate.ts 순수 함수로 빼 두었고 여기에는 배선만 남긴다.
//
// ★ @react-native-firebase를 직접 import하지 않는다 — 격리 계층은 index.ts 하나다.
// 여기서 직접 부르면 전달 경로를 바꿀 때 고쳐야 할 곳이 둘이 되고, 격리가 폴더 단위로
// 헐거워진다(07-27 kakaoLogin.ts와 같은 규칙).
import React from 'react';
import { useQueryClient } from '@tanstack/react-query';

import { navigateFromPush } from '@/navigation/navigationRef';
import { getInitialPush, onPushOpened, onPushReceived, onPushTokenRefresh } from '.';
import { pushInvalidationKeys } from './invalidate';
import { pushTarget, type PushData } from './target';

/**
 * 세 경로를 모두 처리한다:
 *  - onPushReceived   앱을 보고 있을 때 (iOS는 배너를 안 띄운다 — firebase.json에서 의도적으로 비활성)
 *  - onPushOpened     백그라운드에서 배너를 눌렀을 때
 *  - getInitialPush   앱이 꺼진 상태에서 배너를 눌러 켜졌을 때
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
    // 포그라운드: 배너를 띄우지 않고 화면만 갱신한다(실시간 전략 §5의 "새로고침 신호").
    const offMessage = onPushReceived(invalidate);

    // 백그라운드에서 배너 탭.
    const offOpened = onPushOpened(open);

    // 앱이 꺼진 상태에서 배너 탭으로 켜진 경우 — 한 번만 확인한다.
    void getInitialPush().then((data) => {
      if (data) open(data);
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
