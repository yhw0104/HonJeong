// 소켓 이벤트를 목록 캐시에 반영하는 순수 함수들.
// 화면이 아니라 여기에 규칙을 모아 두면 테스트할 수 있다.
import { parseWsEvent, applyMessageToList, applyReadToList } from './applyEvent';

const conv = (over: Partial<ReturnType<typeof baseConv>> = {}) => ({ ...baseConv(), ...over });
function baseConv() {
  return {
    conversationId: 1,
    status: 'ACTIVE' as const,
    partnerUserId: 20,
    partnerNickname: '상대',
    partnerProfileImageUrl: null,
    placeName: '식당',
    lastMessagePreview: '이전',
    lastMessageAt: '2026-08-13T12:00:00',
    unreadCount: 0,
    partnerLastReadAt: null,
    createdAt: '2026-08-13T11:00:00',
    muted: false,
  };
}

describe('parseWsEvent', () => {
  it('message 이벤트를 읽는다', () => {
    const raw = JSON.stringify({
      type: 'message',
      conversationId: 1,
      message: { id: 9, senderUserId: 20, type: 'TEXT', text: '왔어요', imageUrl: null, createdAt: '2026-08-13T13:00:00' },
    });
    expect(parseWsEvent(raw)?.type).toBe('message');
  });

  it('read 이벤트를 읽는다', () => {
    const raw = JSON.stringify({ type: 'read', conversationId: 1, readerUserId: 20, readAt: '2026-08-13T13:05:00' });
    expect(parseWsEvent(raw)?.type).toBe('read');
  });

  it('★깨진 JSON은 null — 서버가 뭘 보내든 앱이 죽으면 안 된다', () => {
    expect(parseWsEvent('이건 JSON이 아니다')).toBeNull();
  });

  it('모르는 type은 null', () => {
    expect(parseWsEvent(JSON.stringify({ type: '새로운거' }))).toBeNull();
  });
});

describe('applyMessageToList', () => {
  const event = {
    type: 'message' as const,
    conversationId: 1,
    message: { id: 9, senderUserId: 20, type: 'TEXT' as const, text: '왔어요', imageUrl: null, createdAt: '2026-08-13T13:00:00' },
  };

  it('해당 대화의 미리보기와 시각을 갱신한다', () => {
    const [next] = applyMessageToList([conv()], event, 10);
    expect(next.lastMessagePreview).toBe('왔어요');
    expect(next.lastMessageAt).toBe('2026-08-13T13:00:00');
  });

  it('★상대가 보낸 메시지는 안읽음을 올린다', () => {
    const [next] = applyMessageToList([conv({ unreadCount: 2 })], event, 10);
    expect(next.unreadCount).toBe(3);
  });

  it('★내가 보낸 메시지는 안읽음을 올리지 않는다 — 내 다른 기기에서 온 것이다', () => {
    const mine = { ...event, message: { ...event.message, senderUserId: 10 } };
    const [next] = applyMessageToList([conv({ unreadCount: 2 })], mine, 10);
    expect(next.unreadCount).toBe(2);
  });

  it('다른 대화는 건드리지 않는다', () => {
    const other = conv({ conversationId: 2, lastMessagePreview: '그대로' });
    const [next] = applyMessageToList([other], event, 10);
    expect(next.lastMessagePreview).toBe('그대로');
  });

  it('목록에 없는 대화면 그대로 둔다 — 재조회가 채운다', () => {
    expect(applyMessageToList([], event, 10)).toEqual([]);
  });
});

describe('applyReadToList', () => {
  it('★상대가 읽었으면 partnerLastReadAt이 갱신된다 — 내 메시지에 읽음이 뜬다', () => {
    const event = { type: 'read' as const, conversationId: 1, readerUserId: 20, readAt: '2026-08-13T13:05:00' };
    const [next] = applyReadToList([conv()], event, 10);
    expect(next.partnerLastReadAt).toBe('2026-08-13T13:05:00');
    expect(next.unreadCount).toBe(0);
  });

  it('★내가 읽었으면 안읽음이 0이 된다 — 내 다른 기기의 배지가 사라진다', () => {
    const event = { type: 'read' as const, conversationId: 1, readerUserId: 10, readAt: '2026-08-13T13:05:00' };
    const [next] = applyReadToList([conv({ unreadCount: 5 })], event, 10);
    expect(next.unreadCount).toBe(0);
    expect(next.partnerLastReadAt).toBeNull();
  });
});
