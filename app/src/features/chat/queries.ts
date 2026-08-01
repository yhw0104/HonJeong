import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert } from 'react-native';
import { ApiError } from '@/shared/api/client';
import { LIVE_REFETCH_MS } from '@/shared/realtime';
import { deleteConversation, fetchConversations, fetchMessages, markConversationRead, sendMessage } from './api';

const CHAT_REFETCH_MS = 5_000;

export const conversationKeys = {
  list: ['chat', 'conversations'] as const,
  messages: (id: number) => ['chat', 'messages', id] as const,
};

export function useConversations() {
  return useQuery({ queryKey: conversationKeys.list, queryFn: fetchConversations, refetchInterval: LIVE_REFETCH_MS });
}

export function useMessages(id: number) {
  return useQuery({
    queryKey: conversationKeys.messages(id),
    queryFn: () => fetchMessages(id),
    refetchInterval: CHAT_REFETCH_MS,
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
      // 서버 메시지를 우선 보여준다(예: 목록 폴링 사이에 진행 중으로 바뀐 409, 이미 삭제된 404).
      // 네트워크 실패·그 외 오류는 서버 메시지가 없으므로 기존 안내문으로 대체한다.
      const message = e instanceof ApiError && e.code !== 'NETWORK_ERROR' ? e.message : '삭제하지 못했어요. 잠시 후 다시 시도해 주세요.';
      Alert.alert('앗', message);
      // 409는 목록이 이미 낡았다는 뜻이므로 즉시 다시 동기화한다 — 폴링 주기(LIVE_REFETCH_MS)까지 기다리면
      // 같은 실패가 반복될 수 있다.
      qc.invalidateQueries({ queryKey: ['chat'] });
    },
  });
}
