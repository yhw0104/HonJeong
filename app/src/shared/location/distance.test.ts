import { distanceMeters, formatDistance, walkingMinutes } from './distance';

describe('distance', () => {
  it('같은 좌표는 0m', () => {
    expect(distanceMeters({ lat: 37.5, lng: 127.0 }, { lat: 37.5, lng: 127.0 })).toBe(0);
  });

  it('연남동~합정 대략 1km대(±300m)', () => {
    const d = distanceMeters({ lat: 37.5639, lng: 126.9256 }, { lat: 37.5495, lng: 126.9136 });
    expect(d).toBeGreaterThan(1500);
    expect(d).toBeLessThan(2500);
  });

  it('포맷: 1000m 미만은 m, 이상은 km', () => {
    expect(formatDistance(320)).toBe('320m');
    expect(formatDistance(1250)).toBe('1.3km');
  });

  it('도보 분: 약 67m/분, 최소 1분', () => {
    expect(walkingMinutes(0)).toBe(1);
    expect(walkingMinutes(30)).toBe(1);
    expect(walkingMinutes(140)).toBe(2);
    expect(walkingMinutes(670)).toBe(10);
  });
});
