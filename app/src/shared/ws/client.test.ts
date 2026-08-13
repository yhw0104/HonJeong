// 소켓 수명 관리. 실제 네트워크 없이 WebSocket을 가짜로 세워 검증한다.
import { createChatSocket } from './client';
import { apiPost } from '@/shared/api/client';

jest.mock('@/shared/api/client', () => ({
  apiPost: jest.fn(async () => ({ ticket: 'T1', expiresInSeconds: 30 })),
}));
jest.mock('@/shared/config/api', () => ({ API_BASE_URL: 'https://test.local' }));

class FakeWebSocket {
  static last: FakeWebSocket | null = null;
  /** 이번 테스트에서 실제로 만들어진 소켓들 — "몇 개가 생겼는가"를 검증할 때 쓴다. */
  static instances: FakeWebSocket[] = [];
  onopen: (() => void) | null = null;
  onclose: (() => void) | null = null;
  onmessage: ((e: { data: string }) => void) | null = null;
  onerror: (() => void) | null = null;
  sent: string[] = [];
  closed = false;
  constructor(public url: string) {
    FakeWebSocket.last = this;
    FakeWebSocket.instances.push(this);
  }
  send(data: string) {
    this.sent.push(data);
  }
  close() {
    this.closed = true;
    this.onclose?.();
  }
}

beforeEach(() => {
  jest.useFakeTimers();
  FakeWebSocket.last = null;
  FakeWebSocket.instances = [];
  (globalThis as unknown as { WebSocket: unknown }).WebSocket = FakeWebSocket;
});

afterEach(() => {
  jest.useRealTimers();
});

/** 티켓 발급(비동기)을 흘려보낸다. */
async function flush() {
  await Promise.resolve();
  await Promise.resolve();
}

describe('createChatSocket', () => {
  it('티켓을 받아 그 주소로 연결한다', async () => {
    const socket = createChatSocket({ onEvent: jest.fn() });
    socket.connect();
    await flush();

    expect(FakeWebSocket.last?.url).toBe('wss://test.local/ws?ticket=T1');
  });

  it('받은 이벤트를 콜백으로 넘긴다', async () => {
    const onEvent = jest.fn();
    const socket = createChatSocket({ onEvent });
    socket.connect();
    await flush();
    FakeWebSocket.last?.onmessage?.({ data: JSON.stringify({ type: 'read', conversationId: 1, readerUserId: 2, readAt: 'x' }) });

    expect(onEvent).toHaveBeenCalledWith(expect.objectContaining({ type: 'read' }));
  });

  it('★깨진 프레임은 콜백을 부르지 않는다', async () => {
    const onEvent = jest.fn();
    const socket = createChatSocket({ onEvent });
    socket.connect();
    await flush();
    FakeWebSocket.last?.onmessage?.({ data: '깨진 것' });

    expect(onEvent).not.toHaveBeenCalled();
  });

  it('★30초마다 ping을 보낸다 — 인터벌이라 두 번째도 온다', async () => {
    const socket = createChatSocket({ onEvent: jest.fn() });
    socket.connect();
    await flush();
    FakeWebSocket.last?.onopen?.();
    // 30초를 두 번 흘려서 확인한다. 한 번만 흘리면 setInterval 대신 setTimeout(1회성)으로
    // 잘못 구현해도 테스트가 통과해버린다.
    jest.advanceTimersByTime(30_000);
    jest.advanceTimersByTime(30_000);

    expect(FakeWebSocket.last?.sent).toEqual(['{"type":"ping"}', '{"type":"ping"}']);
  });

  it('★끊기면 1초 뒤 다시 연결한다', async () => {
    const socket = createChatSocket({ onEvent: jest.fn() });
    socket.connect();
    await flush();
    const first = FakeWebSocket.last;
    first?.onclose?.();
    jest.advanceTimersByTime(1_000);
    await flush();

    expect(FakeWebSocket.last).not.toBe(first);
  });

  it('★티켓 발급이 실패해도 루프가 죽지 않는다 — 백오프 뒤 다시 시도한다', async () => {
    (apiPost as jest.Mock).mockRejectedValueOnce(new Error('네트워크 오류'));

    const socket = createChatSocket({ onEvent: jest.fn() });
    socket.connect();
    await flush();

    expect(FakeWebSocket.last).toBeNull(); // 첫 시도는 티켓 발급 자체가 실패해 소켓이 없다

    jest.advanceTimersByTime(1_000); // 첫 백오프(1초) 경과 — 재시도가 걸린다
    await flush();

    expect(FakeWebSocket.last?.url).toBe('wss://test.local/ws?ticket=T1');
  });

  it('★disconnect 뒤에는 재연결하지 않는다 — 로그아웃·백그라운드에서 되살아나면 안 된다', async () => {
    const socket = createChatSocket({ onEvent: jest.fn() });
    socket.connect();
    await flush();
    const first = FakeWebSocket.last;

    socket.disconnect();
    jest.advanceTimersByTime(60_000);
    await flush();

    expect(FakeWebSocket.last).toBe(first);
  });

  it('★티켓 요청이 아직 끝나지 않았을 때 disconnect하면 소켓을 아예 만들지 않는다', async () => {
    const socket = createChatSocket({ onEvent: jest.fn() });
    socket.connect(); // 티켓 요청을 보냈지만 아직 응답이 오지 않은 상태
    socket.disconnect(); // flush 전에 끊는다 — 응답이 뒤늦게 와도 소켓을 만들면 안 된다
    await flush();
    jest.advanceTimersByTime(60_000);
    await flush();

    expect(FakeWebSocket.last).toBeNull();
  });

  it('★연결 시도가 겹치면 소켓을 하나만 만든다 — wanted만으로는 "이 시도가 최신인가"를 못 가른다', async () => {
    const socket = createChatSocket({ onEvent: jest.fn() });
    socket.connect(); // 세대 1의 티켓 요청이 아직 끝나지 않은 상태
    socket.disconnect(); // 세대 1을 버린다
    socket.connect(); // 세대 2 시작 — 세대 1의 티켓 응답은 이후에 도착한다
    await flush();

    // 버려진 세대 1의 open()이 뒤늦게 재개돼도 소켓을 만들지 않아야, 세대 2 소켓 하나만 남는다.
    expect(FakeWebSocket.instances.length).toBe(1);
  });

  it('★뒤처진 소켓의 close가 늦게 도착해도 최신 연결의 하트비트를 죽이지 않는다', async () => {
    const socket = createChatSocket({ onEvent: jest.fn() });
    socket.connect();
    await flush();
    const first = FakeWebSocket.last;

    socket.disconnect();
    socket.connect();
    await flush();
    const second = FakeWebSocket.last;
    second?.onopen?.(); // 새 세대가 연결되어 하트비트가 돈다

    // 실제 RN의 close는 비동기라, first의 close 이벤트가 이 시점에야 뒤늦게 도착할 수 있다.
    // (세대 구분이 없다면 이 늦은 close가 공유 타이머·ws를 지워 second의 하트비트를 죽인다.)
    first?.onclose?.();

    jest.advanceTimersByTime(30_000);

    expect(second?.sent).toContain('{"type":"ping"}');
  });
});
