import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert } from 'react-native';
import { ApiError } from '@/shared/api/client';
import { LIVE_REFETCH_MS } from '@/shared/realtime';
import { deleteConversation, fetchConversations, fetchMessages, markConversationRead, sendMessage, setConversationMuted } from './api';

// 대화방(메시지) 폴링 주기. WebSocket이 붙은 뒤로는 **안전망**이다 — 소켓이 조용히 죽어도
// 화면이 결국 따라잡게 한다(docs/08-실시간-전략.md §8).
//
// ★대화방만 이 상수를 쓴다. 방은 소켓이 **완전히 덮는다** — 새 메시지는 message 이벤트로,
//   읽음 표시는 read 이벤트로 즉시 들어오므로 폴링을 30초로 늦춰도 잃는 것이 없다.
//   공용 LIVE_REFETCH_MS(15초)를 올려서 맞추지는 않는다. 그 값을 건드리면 지도·통계·
//   혼밥러 목록까지 같이 느려지기 때문에, 채팅방만 자기 상수를 갖는다.
const CHAT_POLL_MS = 30_000;

export const conversationKeys = {
  list: ['chat', 'conversations'] as const,
  messages: (id: number) => ['chat', 'messages', id] as const,
};

// ★대화 목록은 방과 달리 **소켓이 덮지 못한다.** 소켓이 나르는 건 (a)이미 목록에 있는 행에
//   붙는 새 메시지와 (b)읽음 포인터, 이 둘뿐이다. 나머지 목록 필드는 오직 이 폴링으로만 온다:
//   - status가 CLOSED로 바뀌는 것. ChatRoom.tsx가 이 행의 status로 입력창 잠금을 정하므로,
//     30초로 늦추면 대화가 닫힌 뒤에도 최대 30초 동안 계속 쓰고 보내다가 CONVERSATION_CLOSED
//     에러를 맞는다("보냈는데 실패했다"는 최악의 실패 모양이다).
//   - 새로 생긴 대화(매칭 성사). 아직 목록에 없는 행이라 소켓 이벤트로는 만들어지지 않는다.
//   그래서 목록은 공용 LIVE_REFETCH_MS(15초)에 그대로 남긴다.
export function useConversations() {
  return useQuery({ queryKey: conversationKeys.list, queryFn: fetchConversations, refetchInterval: LIVE_REFETCH_MS });
}

export function useMessages(id: number) {
  return useQuery({
    queryKey: conversationKeys.messages(id),
    queryFn: () => fetchMessages(id),
    refetchInterval: CHAT_POLL_MS,
  });
}

export function useSendMessage(id: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (body: { type: 'TEXT'; text: string } | { type: 'IMAGE'; imageUrl: string }) => sendMessage(id, body),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: conversationKeys.messages(id) });
      qc.invalidateQueries({ queryKey: conversationKeys.list });
    },
  });
}

export function useMarkRead(id: number) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => markConversationRead(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: conversationKeys.list }),
  });
}

// 대화방 삭제(내 목록에서만). 삭제 후 목록·안읽음 집계가 바뀌므로 chat 전체를 무효화한다.
export function useDeleteConversation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => deleteConversation(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['chat'] }),
    onError: (e: unknown) => {
      // 서버 메시지를 우선 보여준다(예: 이미 삭제된 404, 그 외 서버측 거절 409).
      // 네트워크 실패·그 외 오류는 서버 메시지가 없으므로 기존 안내문으로 대체한다.
      const message = e instanceof ApiError && e.code !== 'NETWORK_ERROR' ? e.message : '삭제하지 못했어요. 잠시 후 다시 시도해 주세요.';
      Alert.alert('앗', message);
      // 409(CONVERSATION_NOT_CLOSED)는 방어적으로만 처리한다 — ACTIVE 대화는 도메인상 CLOSED로
      // 되돌아갈 수 없으므로(close()는 단방향) 폴링 사이에 실제로 발생하지는 않지만, 만일을 대비해
      // 즉시 다시 동기화해 목록을 서버 상태와 맞춘다.
      qc.invalidateQueries({ queryKey: ['chat'] });
    },
  });
}

// 대화별 알림 끄기/켜기. 표시되는 상태(muted)는 목록 응답이 들고 오므로 별도 조회 없이 목록만 다시 받는다.
export function useSetConversationMuted() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, muted }: { id: number; muted: boolean }) => setConversationMuted(id, muted),
    // 성공·실패 모두 무효화한다(useDeleteConversation과 같은 방침) — 낙관적으로 껐다고 표시한 채
    // 실패가 남으면 "껐다고 보이는데 알림은 오는" 상태가 된다. 그건 고장으로 읽힌다.
    onSettled: () => qc.invalidateQueries({ queryKey: conversationKeys.list }),
    onError: (e: unknown) => {
      const message = e instanceof ApiError && e.code !== 'NETWORK_ERROR' ? e.message : '알림 설정을 바꾸지 못했어요. 잠시 후 다시 시도해 주세요.';
      Alert.alert('앗', message);
    },
  });
}
