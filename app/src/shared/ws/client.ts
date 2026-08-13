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
  /**
   * "연결하고 싶은가"(wanted)와 "지금 이 시도가 최신인가"는 서로 다른 질문이다 — wanted는
   * 불리언이라 둘을 구분하지 못한다. connect→disconnect→connect가 티켓 왕복(await) 안에서
   * 겹치면, 두 open() 호출이 재개되는 시점에 wanted는 똑같이 true라서 뒤처진 시도까지
   * "유효하다"고 착각해 소켓을 두 개 만들고 공유 상태(ws·pingTimer)를 서로 덮어쓴다. 게다가
   * 실제 소켓의 close는 비동기라, 이미 버려진 세대의 onclose가 나중에야 도착해 그 사이 새로
   * 열린(살아 있는) 소켓의 참조와 하트비트를 지워버릴 수도 있다. epoch는 connect·disconnect
   * 때마다 올라가는 "세대 번호"로, "이 시도가 그때 그 세대인지"를 매 비동기 재개 지점·매 소켓
   * 콜백에서 확인해 뒤처진 시도가 최신 상태를 건드리지 못하게 막는다.
   */
  let epoch = 0;
  let retryTimer: ReturnType<typeof setTimeout> | null = null;
  let pingTimer: ReturnType<typeof setInterval> | null = null;

  function clearTimers() {
    if (retryTimer) clearTimeout(retryTimer);
    if (pingTimer) clearInterval(pingTimer);
    retryTimer = null;
    pingTimer = null;
  }

  function scheduleRetry(myEpoch: number) {
    if (!wanted || myEpoch !== epoch) return;
    const delay = reconnectDelayMs(attempt);
    attempt += 1;
    retryTimer = setTimeout(() => {
      void open(myEpoch);
    }, delay);
  }

  async function open(myEpoch: number) {
    if (!wanted || myEpoch !== epoch) return;
    let ticket: string;
    try {
      ticket = (await fetchWsTicket()).ticket;
    } catch {
      // 티켓 발급 실패(네트워크·세션 만료)는 재시도로 흘린다. 세션이 정말 끝났다면
      // AuthContext가 guest로 내려 useChatSocket이 disconnect를 부른다.
      scheduleRetry(myEpoch);
      return;
    }
    // await 이후 재개된 시점 — 그 사이 disconnect·재연결로 세대가 넘어갔을 수 있다.
    if (!wanted || myEpoch !== epoch) return;

    const socket = new WebSocket(toWsUrl(API_BASE_URL, ticket));
    ws = socket;

    socket.onopen = () => {
      if (myEpoch !== epoch) {
        // 이 소켓이 열리는 사이 더 최신 세대가 시작됐다 — 그대로 닫고 ping 타이머는
        // 시작하지 않는다. 안 그러면 아무도 못 지우는 인터벌이 새 세대 것과 뒤섞인다.
        socket.close();
        return;
      }
      attempt = 0;
      pingTimer = setInterval(() => socket.send(PING_FRAME), PING_MS);
    };
    socket.onmessage = (e: { data: string }) => {
      if (myEpoch !== epoch) return; // 뒤처진 세대의 메시지는 무시한다.
      const event = parseWsEvent(String(e.data));
      if (event) onEvent(event);
    };
    socket.onclose = () => {
      // 뒤처진 세대의 close는 공유 상태(ws·타이머)를 건드리면 안 된다 — 실제 RN close는
      // 비동기라 새 세대가 이미 연결된 뒤에 늦게 도착할 수 있다.
      if (myEpoch !== epoch) return;
      clearTimers();
      ws = null;
      scheduleRetry(myEpoch);
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
      epoch += 1;
      void open(epoch);
    },
    disconnect() {
      wanted = false;
      epoch += 1;
      clearTimers();
      ws?.close();
      ws = null;
    },
  };
}
