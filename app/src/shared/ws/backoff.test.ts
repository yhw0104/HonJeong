// 재연결 간격. 즉시 재시도를 반복하면 서버가 죽었을 때 앱이 서버를 두들기고 배터리를 태운다.
import { reconnectDelayMs } from './backoff';

describe('reconnectDelayMs', () => {
  it('첫 시도는 1초', () => {
    expect(reconnectDelayMs(0)).toBe(1_000);
  });

  it('시도마다 두 배로 늘어난다', () => {
    expect(reconnectDelayMs(1)).toBe(2_000);
    expect(reconnectDelayMs(2)).toBe(4_000);
    expect(reconnectDelayMs(3)).toBe(8_000);
    expect(reconnectDelayMs(4)).toBe(16_000);
  });

  it('★30초에서 멈춘다 — 더 늘어나면 복귀가 체감되게 늦어진다', () => {
    expect(reconnectDelayMs(5)).toBe(30_000);
    expect(reconnectDelayMs(50)).toBe(30_000);
  });

  it('음수가 들어와도 1초 아래로 내려가지 않는다', () => {
    expect(reconnectDelayMs(-3)).toBe(1_000);
  });
});
