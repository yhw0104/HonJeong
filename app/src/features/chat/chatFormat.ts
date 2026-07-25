import type { ChatMessage, ConversationStatus, ConversationSummary } from './types';

export const totalUnread = (list: Pick<ConversationSummary, 'unreadCount'>[]): number =>
  list.reduce((sum, c) => sum + (c.unreadCount || 0), 0);

export const messagePreview = (msg: Pick<ChatMessage, 'type' | 'text'> | null): string => {
  if (!msg) return '';
  return msg.type === 'IMAGE' ? '사진' : msg.text ?? '';
};

export const isClosed = (status: ConversationStatus): boolean => status === 'CLOSED';

/** ISO(KST 벽시계) 문자열에서 "HH:MM" 추출. "2026-07-25T14:30:00" → "14:30". */
export const formatTime = (iso: string): string => (iso.length >= 16 ? iso.slice(11, 16) : iso);

/**
 * 내 메시지를 상대가 읽었는지 — 상대의 마지막 읽은 시각이 메시지 시각 이상이면 읽음.
 * 둘 다 KST 벽시계 ISO 문자열(무TZ)이라 같은 방식으로 파싱해 비교하면 오프셋이 상쇄된다.
 */
export const readByPartner = (messageCreatedAt: string, partnerLastReadAt: string | null): boolean =>
  partnerLastReadAt != null &&
  new Date(partnerLastReadAt).getTime() >= new Date(messageCreatedAt).getTime();

/** max 글자를 넘으면 잘라 '…'을 붙인다. "청년다방 수지상현점"(9자), max 8 → "청년다방 수지상…". */
export const truncate = (s: string, max: number): string =>
  Array.from(s).length > max ? Array.from(s).slice(0, max).join('') + '…' : s;
