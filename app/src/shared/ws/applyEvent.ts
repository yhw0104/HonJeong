// 소켓 이벤트를 목록 캐시에 반영하는 규칙.
//
// 화면이나 훅이 아니라 여기에 모아 둔 이유: 이 규칙들이 이 기능에서 가장 틀리기 쉬운 부분인데
// (누가 보냈는지에 따라 안읽음이 갈리고, 누가 읽었는지에 따라 갱신 대상이 갈린다)
// 순수 함수로 떼어 놓으면 소켓 없이 테스트할 수 있다.
import type { ConversationSummary } from '@/features/chat/types';
import type { WsEvent, WsMessageEvent, WsReadEvent } from './types';

/**
 * 소켓이 준 문자열을 이벤트로 읽는다.
 *
 * ★ 서버가 무엇을 보내든 앱이 죽어서는 안 된다 — 깨진 JSON이나 모르는 type은 null로 흘린다.
 */
export function parseWsEvent(raw: string): WsEvent | null {
  let parsed: unknown;
  try {
    parsed = JSON.parse(raw);
  } catch {
    return null;
  }
  if (typeof parsed !== 'object' || parsed === null) return null;
  const type = (parsed as { type?: unknown }).type;
  if (type === 'message' || type === 'read' || type === 'pong') {
    return parsed as WsEvent;
  }
  return null;
}

/**
 * 새 메시지를 대화 목록에 반영한다.
 *
 * ★ 안읽음은 **상대가 보낸 경우에만** 올린다. 내 다른 기기에서 보낸 메시지도 이 이벤트로
 * 들어오는데, 그걸 세면 내가 보낼 때마다 내 안읽음이 늘어난다.
 *
 * @param list 현재 목록 캐시
 * @param event 새 메시지 이벤트
 * @param myUserId 내 사용자 id
 */
export function applyMessageToList(
  list: ConversationSummary[],
  event: WsMessageEvent,
  myUserId: number,
): ConversationSummary[] {
  return list.map((c) => {
    if (c.conversationId !== event.conversationId) return c;
    const fromPartner = event.message.senderUserId !== myUserId;
    return {
      ...c,
      lastMessagePreview: event.message.text ?? c.lastMessagePreview,
      lastMessageAt: event.message.createdAt,
      unreadCount: fromPartner ? c.unreadCount + 1 : c.unreadCount,
    };
  });
}

/**
 * 읽음을 대화 목록에 반영한다.
 *
 * 읽은 사람이 누구냐로 갈린다.
 * - 상대가 읽음 → `partnerLastReadAt` 갱신(내 마지막 메시지에 '읽음'이 뜬다)
 * - 내가 읽음  → `unreadCount`를 0으로(내 다른 기기의 배지가 사라진다)
 *
 * @param list 현재 목록 캐시
 * @param event 읽음 이벤트
 * @param myUserId 내 사용자 id
 */
export function applyReadToList(
  list: ConversationSummary[],
  event: WsReadEvent,
  myUserId: number,
): ConversationSummary[] {
  return list.map((c) => {
    if (c.conversationId !== event.conversationId) return c;
    if (event.readerUserId === myUserId) {
      return { ...c, unreadCount: 0 };
    }
    return { ...c, partnerLastReadAt: event.readAt };
  });
}
