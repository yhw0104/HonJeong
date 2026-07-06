// '이 위치에서 재검색' 버튼 노출 판정 — MapHome이 쓰는 순수 로직.
import { distanceMeters, type Coord } from './distance';
import type { LocationSource } from './pickLocation';

/** 재검색 버튼 노출 문턱(m) — 검색 반경 1km의 20%. */
export const RESEARCH_THRESHOLD_M = 200;

/** 진짜 GPS이고 검색 기준점(anchor)에서 문턱 이상 벗어났을 때만 재검색을 권한다. */
export function shouldOfferResearch(coord: Coord, anchor: Coord, source: LocationSource): boolean {
  if (source !== 'gps') return false;
  return distanceMeters(coord, anchor) >= RESEARCH_THRESHOLD_M;
}
