import { shouldReRequestLocation } from './reRequest';

describe('shouldReRequestLocation', () => {
  it('포그라운드 복귀 + GPS 아직 없음(default) → 재요청', () =>
    expect(shouldReRequestLocation('default', 'active')).toBe(true));
  it('포그라운드 복귀 + 내동네(region) → 재요청', () =>
    expect(shouldReRequestLocation('region', 'active')).toBe(true));
  it('이미 GPS면 재요청 안 함', () =>
    expect(shouldReRequestLocation('gps', 'active')).toBe(false));
  it('백그라운드 전환은 재요청 안 함', () =>
    expect(shouldReRequestLocation('default', 'background')).toBe(false));
  it('inactive 전환은 재요청 안 함', () =>
    expect(shouldReRequestLocation('default', 'inactive')).toBe(false));
});
