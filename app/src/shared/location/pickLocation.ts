import { DEFAULT_MAP_CENTER } from '@/shared/config/kakao';

export type Coord = { lat: number; lng: number };
export type LocationSource = 'gps' | 'region' | 'default';

/** 위치 우선순위: GPS → 저장된 내 동네 → 연남동 기본. (순수 함수) */
export function pickLocation(input: { gps: Coord | null; region: Coord | null }): {
  coord: Coord;
  source: LocationSource;
} {
  if (input.gps) return { coord: input.gps, source: 'gps' };
  if (input.region) return { coord: input.region, source: 'region' };
  return { coord: DEFAULT_MAP_CENTER, source: 'default' };
}
