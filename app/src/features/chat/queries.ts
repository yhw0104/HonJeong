import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { LIVE_REFETCH_MS } from '@/shared/realtime';
import { fetchConversations, fetchMessages, markConversationRead, sendMessage } from './api';

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
