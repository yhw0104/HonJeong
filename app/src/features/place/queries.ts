import { useQuery } from '@tanstack/react-query';
import type { Coord } from '@/shared/location/pickLocation';
import { LIVE_REFETCH_MS } from '@/shared/realtime';
import {
  searchPlaces,
  fetchNearby,
  fetchPlaceDetail,
  fetchPlaceCheckinSummary,
  fetchPlaceMates,
} from './api';

/** 식당 이름 검색. 빈 검색어면 호출하지 않는다. */
export function usePlaceSearch(query: string) {
  const q = query.trim();
  return useQuery({
    queryKey: ['places', 'search', q],
    queryFn: () => searchPlaces(q),
    enabled: q.length > 0,
  });
}

/** 현재 좌표 주변 식당(거리순 + 혼밥러수). 혼밥러수는 실시간이라 기본은 주기 폴링한다.
 *  enabled=false면 호출하지 않는다(중심 좌표가 아직 없을 때 등).
 *  poll=false면 폴링 없이 좌표(queryKey) 변경·재진입 때만 갱신한다(홈 지도 재검색용). */
export function useNearby(coord: Coord, radius = 1000, enabled = true, poll = true) {
  return useQuery({
    queryKey: ['nearby', { lat: coord.lat, lng: coord.lng, radius }],
    queryFn: () => fetchNearby(coord.lat, coord.lng, radius),
    refetchInterval: poll ? LIVE_REFETCH_MS : false,
    enabled,
  });
}

/** 식당 상세 단건. */
export function usePlaceDetail(placeId: number) {
  return useQuery({
    queryKey: ['place', placeId],
    queryFn: () => fetchPlaceDetail(placeId),
  });
}

/** 식당 사회적 증거 요약. 누적 성격이라 폴링 없음(체크인 변경 시 ['place'] 무효화가 커버). */
export function usePlaceCheckinSummary(placeId: number) {
  return useQuery({
    queryKey: ['place', placeId, 'checkin-summary'],
    queryFn: () => fetchPlaceCheckinSummary(placeId),
  });
}

/** 식당별 메이트(같이 먹기 매칭 대상) 목록. */
export function usePlaceMates(placeId: number) {
  return useQuery({
    queryKey: ['place', placeId, 'mates'],
    queryFn: () => fetchPlaceMates(placeId),
  });
}
