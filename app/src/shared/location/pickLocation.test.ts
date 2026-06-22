import { pickLocation } from './pickLocation';
import { DEFAULT_MAP_CENTER } from '@/shared/config/kakao';

const gps = { lat: 37.5, lng: 127.0 };
const region = { lat: 37.49, lng: 127.03 };

describe('pickLocation', () => {
  it('GPS 있으면 GPS(source=gps)', () => {
    expect(pickLocation({ gps, region })).toEqual({ coord: gps, source: 'gps' });
  });
  it('GPS 없고 저장동네 있으면 region', () => {
    expect(pickLocation({ gps: null, region })).toEqual({ coord: region, source: 'region' });
  });
  it('둘 다 없으면 연남동 기본(source=default)', () => {
    expect(pickLocation({ gps: null, region: null })).toEqual({
      coord: DEFAULT_MAP_CENTER,
      source: 'default',
    });
  });
});
