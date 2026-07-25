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

/** max 글자를 넘으면 잘라 '…'을 붙인다. "청년다방 수지상현점"(10자), max 8 → "청년다방 수지상…". */
export const truncate = (s: string, max: number): string =>
  Array.from(s).length > max ? Array.from(s).slice(0, max).join('') + '…' : s;

const pad2 = (n: number): string => String(n).padStart(2, '0');
const localDateStr = (d: Date): string => `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;

/**
 * 대화목록 마지막 대화 시각: 오늘이면 시간(HH:MM), 당일이 지나면 날짜(올해=M월 D일 / 해 넘으면 YYYY. M. D).
 * iso·now 모두 KST 기준(앱 전역 KST). now = 비교용 현재 시각.
 */
export const formatListTime = (iso: string | null, now: Date): string => {
  if (!iso) return '';
  const isoDate = iso.slice(0, 10); // "YYYY-MM-DD"
  if (isoDate === localDateStr(now)) return formatTime(iso);
  const [y, m, d] = isoDate.split('-').map(Number);
  return y === now.getFullYear() ? `${m}월 ${d}일` : `${y}. ${m}. ${d}`;
};
