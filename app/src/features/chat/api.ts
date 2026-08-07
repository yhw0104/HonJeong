import { apiGet, apiPost, apiPatch, apiDelete } from '@/shared/api/client';
import type { ChatMessage, ConversationSummary } from './types';

export const fetchConversations = () => apiGet<ConversationSummary[]>('/conversations');
export const fetchMessages = (id: number) => apiGet<ChatMessage[]>(`/conversations/${id}/messages`);
export const sendMessage = (id: number, body: { type: 'TEXT'; text: string } | { type: 'IMAGE'; imageUrl: string }) =>
  apiPost<ChatMessage>(`/conversations/${id}/messages`, body);
export const markConversationRead = (id: number) => apiPost<null>(`/conversations/${id}/read`, {});
/** 대화방을 내 목록에서만 삭제(소프트). 종료된 대화만 가능 — 진행 중이면 409. */
export const deleteConversation = (id: number) => apiDelete<null>(`/conversations/${id}`);
/** 이 대화의 푸시 알림 켜기/끄기. 종료된 대화도 토글할 수 있다(알림을 끄는 건 삭제와 달리 제약이 없다).
 *  참여자가 아니면 404(CONVERSATION_NOT_FOUND) — 남의 대화는 존재 자체를 알리지 않는다. */
export const setConversationMuted = (id: number, muted: boolean) =>
  apiPatch<null>(`/conversations/${id}/mute`, { muted });
