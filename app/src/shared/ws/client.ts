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

/**
 * 하트비트 주기(ms).
 *
 * ★서버의 유휴 세션 타임아웃(WebSocketConfig.SESSION_IDLE_TIMEOUT_MS = 60초)과 한 쌍이다.
 *  한쪽만 바꾸면 멀쩡한 연결이 조용히 끊긴다.
 */
const PING_MS = 30_000;
const PING_FRAME = '{"type":"ping"}';

/**
 * ping을 보낸 뒤 pong(또는 아무 프레임이든)을 기다리는 한계 시간(ms).
 *
 * ★이게 pong이 존재하는 유일한 이유다 — half-open 감지. 서버가 사라졌는데 TCP가 아직 눈치채지
 *  못하면 `onclose`가 **영원히 안 온다.** onclose가 안 오면 scheduleRetry도 안 돌아서, 소켓은
 *  아무 데도 연결돼 있지 않은 채로 "연결됨" 상태에 갇힌다(화면이 조용히 멈춘다). 그래서 앱이
 *  스스로 판정해 끊고, 평소의 onclose → scheduleRetry 경로를 태운다.
 *
 *  PING_MS(30초)보다 넉넉히 짧아야 한다 — 안 그러면 다음 ping이 이전 deadline을 밀어내며
 *  판정이 영원히 유예된다.
 */
const PONG_TIMEOUT_MS = 10_000;

type Options = {
  onEvent: (event: WsEvent) => void;
  /**
   * 연결이 (재)수립됐을 때. 끊겼던 사이의 갱(gap)을 메우는 자리다 — 소켓은 끊겨 있던 동안의
   * 메시지를 나중에 채워 주지 않으므로, 여기서 전량 재조회를 걸지 않으면 그 구간의 메시지가
   * 다음 폴링 때까지 안 보인다(설계 문서 §8·§14).
   */
  onOpen?: () => void;
};

/**
 * 소켓 하나를 만들고 그 수명을 관리한다.
 *
 * @param onEvent 수신 이벤트 콜백(깨진 프레임은 여기까지 오지 않는다)
 * @param onOpen 연결 성공 콜백(재연결마다 불린다)
 * @returns connect/disconnect
 */
export function createChatSocket({ onEvent, onOpen }: Options) {
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
  /** ping을 보낸 뒤 응답을 기다리는 시한. 프레임이 하나라도 오면 지운다. */
  let pongTimer: ReturnType<typeof setTimeout> | null = null;

  function clearPongDeadline() {
    if (pongTimer) clearTimeout(pongTimer);
    pongTimer = null;
  }

  function clearTimers() {
    if (retryTimer) clearTimeout(retryTimer);
    if (pingTimer) clearInterval(pingTimer);
    retryTimer = null;
    pingTimer = null;
    clearPongDeadline();
  }

  /**
   * ping을 보낸 직후 시한을 건다. 시한 안에 아무 프레임도 안 오면 half-open으로 보고 직접 닫는다.
   *
   * 판정 기준을 "pong이 왔는가"가 아니라 "아무 프레임이든 왔는가"로 둔다 — 메시지가 흐르고
   * 있다는 것 자체가 연결이 살아 있다는 더 강한 증거이고, 서버가 pong 규약을 바꿔도 살아 있는
   * 연결을 끊지 않는다.
   */
  function armPongDeadline(myEpoch: number, socket: WebSocket) {
    clearPongDeadline(); // 이전 시한이 남아 있으면 지운다 — 안 그러면 참조를 잃은 타이머가 뒤늦게 닫는다.
    pongTimer = setTimeout(() => {
      if (myEpoch !== epoch) return; // 뒤처진 세대의 시한은 최신 소켓을 건드리면 안 된다.
      pongTimer = null;
      // close()가 onclose를 부르고, 거기서 clearTimers → scheduleRetry가 평소대로 돈다.
      socket.close();
    }, PONG_TIMEOUT_MS);
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
      pingTimer = setInterval(() => {
        socket.send(PING_FRAME);
        armPongDeadline(myEpoch, socket);
      }, PING_MS);
      // 갱 복구는 여기서만 걸 수 있다 — 포그라운드 복귀뿐 아니라 WiFi↔LTE 전환처럼 앱이 계속
      // 떠 있는 채로 소켓만 끊겼다 붙는 경우까지 덮어야 한다(그때는 AppState 이벤트가 없다).
      onOpen?.();
    };
    socket.onmessage = (e: { data: string }) => {
      if (myEpoch !== epoch) return; // 뒤처진 세대의 메시지는 무시한다.
      clearPongDeadline(); // 프레임이 왔다 = 연결이 살아 있다(pong이든 message든 상관없다).
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
