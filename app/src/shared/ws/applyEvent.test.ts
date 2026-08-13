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

  it('★사진 메시지는 미리보기가 이전 텍스트에 머물지 않고 "사진"이 된다 — 서버 목록과 일치', () => {
    const image = {
      type: 'message' as const,
      conversationId: 1,
      message: { id: 9, senderUserId: 20, type: 'IMAGE' as const, text: null, imageUrl: 'https://x/1.jpg', createdAt: '2026-08-13T13:00:00' },
    };
    const [next] = applyMessageToList([conv({ lastMessagePreview: '안녕' })], image, 10);
    expect(next.lastMessagePreview).toBe('사진');
  });

  it('다른 대화는 건드리지 않는다', () => {
    const other = conv({ conversationId: 2, lastMessagePreview: '그대로' });
    const [next] = applyMessageToList([other], event, 10);
    expect(next.lastMessagePreview).toBe('그대로');
  });

  it('목록에 없는 대화면 그대로 둔다 — 재조회가 채운다', () => {
    expect(applyMessageToList([], event, 10)).toEqual([]);
  });

  it('★아래쪽 대화에 메시지가 오면 맨 위로 올라온다 — 제자리 갱신만 하면 순서가 뒤집힌다', () => {
    const top = conv({ conversationId: 2, lastMessageAt: '2026-08-13T12:30:00' });
    const bottom = conv({ conversationId: 1, lastMessageAt: '2026-08-13T12:00:00' });

    const next = applyMessageToList([top, bottom], event, 10); // event는 conversationId=1, 13:00

    expect(next.map((c) => c.conversationId)).toEqual([1, 2]);
  });

  it('★메시지가 아직 없는 대화는 createdAt으로 줄을 선다 — 서버의 COALESCE와 같은 규칙', () => {
    // 방금 매칭돼 lastMessageAt이 null인 대화(createdAt 14:00)가 메시지 13:00짜리보다 위다.
    const fresh = conv({ conversationId: 3, lastMessageAt: null, createdAt: '2026-08-13T14:00:00' });
    const target = conv({ conversationId: 1, lastMessageAt: '2026-08-13T12:00:00' });

    const next = applyMessageToList([target, fresh], event, 10);

    expect(next.map((c) => c.conversationId)).toEqual([3, 1]);
  });

  it('★활동 시각이 같으면 id가 큰 쪽이 위 — 서버의 id DESC tie-break와 같다', () => {
    const a = conv({ conversationId: 5, lastMessageAt: '2026-08-13T13:00:00' });
    const b = conv({ conversationId: 9, lastMessageAt: '2026-08-13T13:00:00' });

    const next = applyMessageToList([a, b], { ...event, conversationId: 5 }, 10);

    expect(next.map((c) => c.conversationId)).toEqual([9, 5]);
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
