// '이 위치에서 재검색' 버튼 노출 판정 — MapHome이 쓰는 순수 로직.
import { distanceMeters, type Coord } from './distance';

/** 재검색 버튼 노출 문턱(m) — 검색 반경 1km의 20%. */
export const RESEARCH_THRESHOLD_M = 200;

/**
 * 사용자가 지도를 드래그해 지도 중심(mapCenter)이 검색 기준점(anchor)에서 문턱 이상 벗어났을 때 재검색을 권한다.
 * GPS 이동이 아니라 '지도 이동' 기준이다 — 위치는 파란 점으로만 보여주고, 재검색은 지도를 움직였을 때만.
 */
export function shouldOfferResearch(mapCenter: Coord, anchor: Coord): boolean {
  return distanceMeters(mapCenter, anchor) >= RESEARCH_THRESHOLD_M;
}
