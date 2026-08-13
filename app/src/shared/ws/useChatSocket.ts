// 소켓을 앱 수명에 붙인다.
//
// 언제 연결하나: 로그인 상태(authed)이고 앱이 포그라운드일 때만.
// 백그라운드에서 소켓을 붙들면 배터리를 태우고, 어차피 OS가 곧 끊는다.
import { useEffect, useRef } from 'react';
import { AppState } from 'react-native';
import { useQueryClient } from '@tanstack/react-query';

import { useAuth } from '@/shared/auth/AuthContext';
import { useMyProfile } from '@/features/users/queries';
import { conversationKeys } from '@/features/chat/queries';
import { createChatSocket } from './client';
import { applyMessageToList, applyReadToList } from './applyEvent';

export function useChatSocket(): void {
  const { status } = useAuth();
  const qc = useQueryClient();
  const { data: me } = useMyProfile();
  const myUserId = me?.id ?? null;

  // 콜백이 매 렌더 새로 만들어져도 소켓을 다시 만들지 않도록 최신 값을 ref로 본다.
  const myUserIdRef = useRef(myUserId);
  myUserIdRef.current = myUserId;

  useEffect(() => {
    if (status !== 'authed') return;

    const socket = createChatSocket({
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

    function sync(active: boolean) {
      if (active) {
        socket.connect();
        // ★재연결 시 갱 복구 — 끊겨 있던 사이의 메시지는 소켓이 채워 주지 못한다.
        //   이게 없으면 백그라운드에 다녀온 뒤의 메시지가 폴링(30초) 전까지 안 보인다.
        qc.invalidateQueries({ queryKey: ['chat'] });
      } else {
        socket.disconnect();
      }
    }

    sync(AppState.currentState === 'active');
    const sub = AppState.addEventListener('change', (s) => sync(s === 'active'));

    return () => {
      sub.remove();
      socket.disconnect();
    };
  }, [status, qc]);
}
