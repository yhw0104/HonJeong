import { formatDistance, formatElapsed } from './format';

describe('formatDistance', () => {
  it('1000m 미만은 m', () => expect(formatDistance(120)).toBe('120m'));
  it('1000m 이상은 km 소수1', () => expect(formatDistance(1500)).toBe('1.5km'));
  it('정확히 1000m는 1.0km', () => expect(formatDistance(1000)).toBe('1.0km'));
});

describe('formatElapsed', () => {
  it('60분 미만은 분', () => expect(formatElapsed(25)).toBe('25분째'));
  it('60분 이상은 시간', () => expect(formatElapsed(90)).toBe('1시간째'));
});
