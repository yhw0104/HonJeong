// 소켓을 앱 수명에 붙인다.
//
// 언제 연결하나: 로그인 상태(authed)이고 앱이 포그라운드일 때만.
// 백그라운드에서 소켓을 붙들면 배터리를 태우고, 어차피 OS가 곧 끊는다.
import { useEffect, useRef } from 'react';
import { AppState, type AppStateStatus } from 'react-native';
import { useQueryClient } from '@tanstack/react-query';

import { useAuth } from '@/shared/auth/AuthContext';
import { useMyProfile } from '@/features/users/queries';
import { conversationKeys } from '@/features/chat/queries';
import { createChatSocket } from './client';
import { applyMessageToList, applyReadToList } from './applyEvent';

/**
 * 이 앱 상태에서 소켓을 붙들고 있어야 하는가.
 *
 * ★'inactive'는 끊지 않는다. iOS에서 'inactive'는 제어센터를 내릴 때, 앱 스위처를 띄울 때,
 *  전화 배너가 뜰 때, 권한 팝업이 뜰 때마다 스쳐 가는 **일상적이고 일시적인** 상태다. 이걸
 *  백그라운드와 같이 취급하면 그때마다 소켓 teardown + 티켓 POST + 핸드셰이크 + 전량 재조회가
 *  통째로 한 번 돈다. 배터리를 태우는 건 진짜 백그라운드뿐이므로 거기서만 끊는다.
 *
 * shared/realtime.ts의 appStateToFocused와 기준이 다른 것은 의도적이다 — 그쪽은 React Query
 * 포커스 판정이라 "화면을 실제로 보고 있는가"가 맞고(‘inactive’는 보고 있지 않다), 여기는
 * "연결을 유지할 가치가 있는가"라 기준이 다르다.
 */
function shouldHoldSocket(state: AppStateStatus): boolean {
  return state !== 'background';
}

export function useChatSocket(): void {
  const { status } = useAuth();
  const qc = useQueryClient();
  // ★이 훅은 App.tsx의 PushBridge(로그인 여부와 무관하게 항상 마운트)에서 불린다 — enabled를
  // 안 걸면 guest·loading 상태(로그인 전, 로그아웃 직후)에도 GET /users/me가 나가고,
  // 401 → refresh 실패(refresh 토큰도 없음) → notifySessionExpired → revokePushToken·
  // clearTokens 순으로 캐스케이드가 돈다. push/index.ts의 onPushTokenRefresh가 같은 이유로
  // getAccessToken() 가드를 두고 있는 것과 동일한 위험 — 여기서는 authed일 때만 조회한다.
  const { data: me } = useMyProfile({ enabled: status === 'authed' });
  const myUserId = me?.id ?? null;

  // 콜백이 매 렌더 새로 만들어져도 소켓을 다시 만들지 않도록 최신 값을 ref로 본다.
  const myUserIdRef = useRef(myUserId);
  myUserIdRef.current = myUserId;

  useEffect(() => {
    if (status !== 'authed') return;

    const socket = createChatSocket({
      // ★소켓이 실제로 (재)연결된 순간의 갱 복구. AppState 훅만으로는 앱이 포그라운드에 머문 채
      //   소켓만 끊겼다 붙는 경우(WiFi↔LTE 핸드오프, 터널, 서버 재기동)를 못 잡는다 — 그때는
      //   AppState 이벤트가 아예 발생하지 않아, 끊긴 사이의 메시지가 폴링 전까지 통째로 사라진다.
      onOpen: () => {
        qc.invalidateQueries({ queryKey: ['chat'] });
      },
      onEvent: (event) => {
        const uid = myUserIdRef.current;
        // 내 프로필이 아직 로딩 중이면 uid가 null이다 — 잘못된 값(0 등)으로 안읽음·읽음을
        // 잘못 귀속시키는 대신 이 이벤트는 그냥 버린다. 뒤이은 폴링(30초)이나 다음 이벤트가
        // 캐시를 따라잡는다.
        if (event.type === 'pong' || uid == null) return;

        if (event.type === 'message') {
          // 방 캐시: 그 대화를 보고 있으면 즉시 새 메시지가 붙는다.
          qc.invalidateQueries({ queryKey: conversationKeys.messages(event.conversationId) });
          qc.setQueryData(conversationKeys.list, (old: unknown) =>
            Array.isArray(old) ? applyMessageToList(old, event, uid) : old);
          return;
        }
        qc.setQueryData(conversationKeys.list, (old: unknown) =>
          Array.isArray(old) ? applyReadToList(old, event, uid) : old);
      },
    });

    // 지금 붙들고 있는 상태인가. 상태가 실제로 바뀔 때만 움직이려고 기억한다 — 'inactive'를
    // 연결 유지로 바꾼 뒤에는 active↔inactive를 오갈 때마다 sync(true)가 연달아 들어오는데,
    // 그때마다 전량 재조회를 걸면 끊지 않기로 한 이득을 재조회 비용으로 되돌려주게 된다.
    let held = false;

    function sync(hold: boolean) {
      if (hold === held) return;
      held = hold;
      if (hold) {
        socket.connect();
        // ★백그라운드에서 돌아왔을 때의 갱 복구. 소켓 재연결 자체의 복구는 위 onOpen이 맡는다
        //   (여기서만 하면 앱이 포그라운드에 머문 채 끊긴 경우를 놓친다).
        qc.invalidateQueries({ queryKey: ['chat'] });
      } else {
        socket.disconnect();
      }
    }

    sync(shouldHoldSocket(AppState.currentState));
    const sub = AppState.addEventListener('change', (s) => sync(shouldHoldSocket(s)));

    return () => {
      sub.remove();
      socket.disconnect();
    };
  }, [status, qc]);
}
