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
    // type만 확인하고 나머지 필드는 그대로 신뢰한다(깊은 검증 없음) — 서버만이 유일한 발신자라는
    // 전제의 의도적인 신뢰 경계다. 여기를 "검증 끝났다"는 뜻으로 오해하지 말 것.
    return parsed as WsEvent;
  }
  return null;
}

/**
 * 목록 정렬 기준 — 서버의 `ConversationRepository.findAllForUser`와 **글자 그대로 같아야 한다**:
 * `ORDER BY COALESCE(lastMessageAt, createdAt) DESC, id DESC`.
 *
 * 서버가 내려보내는 시각은 전부 같은 형식의 ISO-8601 LocalDateTime 문자열이라 사전순 비교가
 * 곧 시간순 비교다(자리수가 고정이고 소수부는 뒤에 붙는다). `Date`로 파싱하지 않는 이유는
 * 타임존이 없는 문자열의 해석이 런타임마다 갈릴 수 있어 오히려 위험하기 때문이다.
 */
function byLastActivityDesc(a: ConversationSummary, b: ConversationSummary): number {
  const at = a.lastMessageAt ?? a.createdAt;
  const bt = b.lastMessageAt ?? b.createdAt;
  if (at !== bt) return at < bt ? 1 : -1;
  return b.conversationId - a.conversationId; // 동시각 tie-break도 서버(id DESC)와 맞춘다.
}

/**
 * 새 메시지를 대화 목록에 반영한다.
 *
 * ★ 안읽음은 **상대가 보낸 경우에만** 올린다. 내 다른 기기에서 보낸 메시지도 이 이벤트로
 * 들어오는데, 그걸 세면 내가 보낼 때마다 내 안읽음이 늘어난다.
 *
 * ★ 반영 후 **다시 정렬한다.** 제자리 갱신만 하면, 맨 위가 아닌 대화에 메시지가 오는 순간
 * 그 행은 있던 자리에 남은 채 시각만 최신으로 바뀐다 — 목록에 "위쪽이 더 오래된" 뒤집힌 순서가
 * 보이고, 다음 폴링이 올 때까지 그대로다.
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
  const next = list.map((c) => {
    if (c.conversationId !== event.conversationId) return c;
    const fromPartner = event.message.senderUserId !== myUserId;
    // '사진' 문자열은 서버 ConversationService.previewsOf()의 목록 미리보기 규칙과 맞춰야 한다.
    // 소켓 이벤트는 미리보기 문자열이 아니라 메시지 DTO를 그대로 들고 오므로, 클라이언트가
    // 이 규칙을 다시 구현해야 한다 — REST 재조회(30초 폴링) 전까지 화면이 서버와 어긋나지 않도록.
    // (PushMessages.chatPreview()의 '사진을 보냈어요'는 푸시 알림 문구라 다른 규칙 — 섞지 말 것)
    const preview = event.message.type === 'IMAGE' ? '사진' : (event.message.text ?? c.lastMessagePreview);
    return {
      ...c,
      lastMessagePreview: preview,
      lastMessageAt: event.message.createdAt,
      unreadCount: fromPartner ? c.unreadCount + 1 : c.unreadCount,
    };
  });
  // map이 이미 새 배열을 만들었으므로 그 위에서 정렬해도 캐시 원본을 건드리지 않는다.
  return next.sort(byLastActivityDesc);
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
