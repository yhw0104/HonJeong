import { appStateToFocused, LIVE_REFETCH_MS } from './realtime';

describe('appStateToFocused — RN AppState를 React Query 포커스 여부로 변환', () => {
  it("'active'(포그라운드)면 focused=true", () => {
    expect(appStateToFocused('active')).toBe(true);
  });

  it("'background'(백그라운드)면 focused=false", () => {
    expect(appStateToFocused('background')).toBe(false);
  });

  it("'inactive'(전환 중)면 focused=false", () => {
    expect(appStateToFocused('inactive')).toBe(false);
  });
});

it('라이브 쿼리 폴링 주기가 양수로 설정돼 있다', () => {
  expect(LIVE_REFETCH_MS).toBeGreaterThan(0);
});
