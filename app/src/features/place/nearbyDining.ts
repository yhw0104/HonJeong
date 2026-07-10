// 검색 첫 화면 "지금 주변에서 혼밥 중" 목록용 순수 로직.
// useNearby가 준 주변 식당들에서, 지금 혼밥 인원이 있는 곳만 가까운 순으로 추린다.
import type { PlaceNearbyItem } from './api';

/** 주변 식당 중 혼밥 인원(activeCount)>0인 곳만, 가까운 순, 최대 limit개. 입력은 변형하지 않는다. */
export function nearbyDiningPlaces(items: PlaceNearbyItem[], limit: number): PlaceNearbyItem[] {
  return items
    .filter((p) => p.activeCount > 0)
    .sort((a, b) => a.distanceMeters - b.distanceMeters)
    .slice(0, limit);
}
