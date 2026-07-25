import { apiGet, apiPost } from '@/shared/api/client';
import type { ChatMessage, ConversationSummary } from './types';

export const fetchConversations = () => apiGet<ConversationSummary[]>('/conversations');
export const fetchMessages = (id: number) => apiGet<ChatMessage[]>(`/conversations/${id}/messages`);
export const sendMessage = (id: number, body: { type: 'TEXT'; text: string } | { type: 'IMAGE'; imageUrl: string }) =>
  apiPost<ChatMessage>(`/conversations/${id}/messages`, body);
export const markConversationRead = (id: number) => apiPost<null>(`/conversations/${id}/read`, {});
