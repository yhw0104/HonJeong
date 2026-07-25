import type { ChatMessage, ConversationStatus, ConversationSummary } from './types';

export const totalUnread = (list: Pick<ConversationSummary, 'unreadCount'>[]): number =>
  list.reduce((sum, c) => sum + (c.unreadCount || 0), 0);

export const messagePreview = (msg: Pick<ChatMessage, 'type' | 'text'> | null): string => {
  if (!msg) return '';
  return msg.type === 'IMAGE' ? '사진' : msg.text ?? '';
};

export const isClosed = (status: ConversationStatus): boolean => status === 'CLOSED';
