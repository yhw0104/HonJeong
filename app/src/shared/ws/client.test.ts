// 소켓 수명 관리. 실제 네트워크 없이 WebSocket을 가짜로 세워 검증한다.
import { createChatSocket } from './client';

jest.mock('@/shared/api/client', () => ({
  apiPost: jest.fn(async () => ({ ticket: 'T1', expiresInSeconds: 30 })),
}));
jest.mock('@/shared/config/api', () => ({ API_BASE_URL: 'https://test.local' }));

class FakeWebSocket {
  static last: FakeWebSocket | null = null;
  onopen: (() => void) | null = null;
  onclose: (() => void) | null = null;
  onmessage: ((e: { data: string }) => void) | null = null;
  onerror: (() => void) | null = null;
  sent: string[] = [];
  closed = false;
  constructor(public url: string) {
    FakeWebSocket.last = this;
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

  it('★30초마다 ping을 보낸다', async () => {
    const socket = createChatSocket({ onEvent: jest.fn() });
    socket.connect();
    await flush();
    FakeWebSocket.last?.onopen?.();
    jest.advanceTimersByTime(30_000);

    expect(FakeWebSocket.last?.sent).toContain('{"type":"ping"}');
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
});
