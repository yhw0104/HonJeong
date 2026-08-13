// 재연결 간격(지수 백오프).
//
// 왜 필요한가: 연결이 끊겼을 때 즉시 재시도를 반복하면, 서버가 내려간 동안 앱이 서버를 두들기고
// 사용자 배터리를 태운다. 반대로 간격이 너무 길면 잠깐 끊겼다 돌아왔을 때 화면이 오래 멈춘다.

/** 첫 재시도까지의 간격(ms). */
const BASE_MS = 1_000;
/** 아무리 오래 실패해도 이보다 길게 기다리지 않는다(ms). */
const MAX_MS = 30_000;

/**
 * 몇 번째 재시도인지로 대기 시간을 정한다.
 *
 * @param attempt 0부터 시작하는 재시도 횟수
 * @returns 기다릴 시간(ms)
 */
export function reconnectDelayMs(attempt: number): number {
  const safe = Math.max(0, attempt);
  return Math.min(MAX_MS, BASE_MS * 2 ** safe);
}
