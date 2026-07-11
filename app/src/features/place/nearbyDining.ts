// 검색 첫 화면 "지금 주변에서 같이 먹을 사람 구하는 중" 목록용 순수 로직.
// useNearby가 준 주변 식당들에서, 지금 모집중(같이 먹을 사람을 구하는) 인원이 있는 곳만 가까운 순으로 추린다.
import type { PlaceNearbyItem } from './api';

/**
 * 주변 식당 중 모집중 인원(seekingCount)>0인 곳만, 가까운 순, 최대 limit개. 입력은 변형하지 않는다.
 * selfPlaceId: 본인이 지금 체크인한 식당은 그 seekingCount에 본인도 포함돼 있으므로 인원에서 본인만 뺀다
 *   (식당 자체는 남겨 다른 모집중 인원은 보이게 함). 본인이 유일했다면 0명이 되어 자연히 목록에서 사라진다.
 */
export function nearbyDiningPlaces(
  items: PlaceNearbyItem[],
  limit: number,
  selfPlaceId?: number | null,
): PlaceNearbyItem[] {
  return items
    .map((p) => (p.placeId === selfPlaceId ? { ...p, seekingCount: p.seekingCount - 1 } : p))
    .filter((p) => p.seekingCount > 0)
    .sort((a, b) => a.distanceMeters - b.distanceMeters)
    .slice(0, limit);
}
