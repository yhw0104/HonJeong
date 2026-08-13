// 채팅 소켓의 수명 관리 — 연결·재연결·하트비트.
//
// 이 소켓은 **수신 전용**이다. 메시지 전송은 REST(POST /conversations/{id}/messages)가 한다.
// 그래서 소켓이 끊겨 있어도 보내기는 계속 동작한다.
import { API_BASE_URL } from '@/shared/config/api';
import { fetchWsTicket } from './api';
import { reconnectDelayMs } from './backoff';
import { parseWsEvent } from './applyEvent';
import { toWsUrl } from './wsUrl';
import type { WsEvent } from './types';

/** 하트비트 주기(ms). */
const PING_MS = 30_000;
const PING_FRAME = '{"type":"ping"}';

type Options = { onEvent: (event: WsEvent) => void };

/**
 * 소켓 하나를 만들고 그 수명을 관리한다.
 *
 * @param onEvent 수신 이벤트 콜백(깨진 프레임은 여기까지 오지 않는다)
 * @returns connect/disconnect
 */
export function createChatSocket({ onEvent }: Options) {
  let ws: WebSocket | null = null;
  let attempt = 0;
  /** disconnect가 불린 뒤에는 재연결하지 않는다. 로그아웃·백그라운드에서 되살아나면 안 된다. */
  let wanted = false;
  let retryTimer: ReturnType<typeof setTimeout> | null = null;
  let pingTimer: ReturnType<typeof setInterval> | null = null;

  function clearTimers() {
    if (retryTimer) clearTimeout(retryTimer);
    if (pingTimer) clearInterval(pingTimer);
    retryTimer = null;
    pingTimer = null;
  }

  function scheduleRetry() {
    if (!wanted) return;
    const delay = reconnectDelayMs(attempt);
    attempt += 1;
    retryTimer = setTimeout(() => {
      void open();
    }, delay);
  }

  async function open() {
    if (!wanted) return;
    let ticket: string;
    try {
      ticket = (await fetchWsTicket()).ticket;
    } catch {
      // 티켓 발급 실패(네트워크·세션 만료)는 재시도로 흘린다. 세션이 정말 끝났다면
      // AuthContext가 guest로 내려 useChatSocket이 disconnect를 부른다.
      scheduleRetry();
      return;
    }
    if (!wanted) return;

    const socket = new WebSocket(toWsUrl(API_BASE_URL, ticket));
    ws = socket;

    socket.onopen = () => {
      attempt = 0;
      pingTimer = setInterval(() => socket.send(PING_FRAME), PING_MS);
    };
    socket.onmessage = (e: { data: string }) => {
      const event = parseWsEvent(String(e.data));
      if (event) onEvent(event);
    };
    socket.onclose = () => {
      clearTimers();
      ws = null;
      scheduleRetry();
    };
    socket.onerror = () => {
      // onclose가 뒤따라 오므로 여기서는 아무것도 하지 않는다(재연결을 두 번 걸지 않기 위해).
    };
  }

  return {
    connect() {
      if (wanted) return;
      wanted = true;
      attempt = 0;
      void open();
    },
    disconnect() {
      wanted = false;
      clearTimers();
      ws?.close();
      ws = null;
    },
  };
}
