import { apiGet, apiPost, apiDelete } from '@/shared/api/client';
import type { ChatMessage, ConversationSummary } from './types';

export const fetchConversations = () => apiGet<ConversationSummary[]>('/conversations');
export const fetchMessages = (id: number) => apiGet<ChatMessage[]>(`/conversations/${id}/messages`);
export const sendMessage = (id: number, body: { type: 'TEXT'; text: string } | { type: 'IMAGE'; imageUrl: string }) =>
  apiPost<ChatMessage>(`/conversations/${id}/messages`, body);
export const markConversationRead = (id: number) => apiPost<null>(`/conversations/${id}/read`, {});
/** 대화방을 내 목록에서만 삭제(소프트). 종료된 대화만 가능 — 진행 중이면 409. */
export const deleteConversation = (id: number) => apiDelete<null>(`/conversations/${id}`);
