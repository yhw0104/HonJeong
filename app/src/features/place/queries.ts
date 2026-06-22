import { useQuery } from '@tanstack/react-query';
import type { Coord } from '@/shared/location/pickLocation';
import { searchPlaces, fetchNearby, fetchPlaceDetail } from './api';

/** 식당 이름 검색. 빈 검색어면 호출하지 않는다. */
export function usePlaceSearch(query: string) {
  const q = query.trim();
  return useQuery({
    queryKey: ['places', 'search', q],
    queryFn: () => searchPlaces(q),
    enabled: q.length > 0,
  });
}

/** 현재 좌표 주변 식당(거리순 + 혼밥러수). */
export function useNearby(coord: Coord, radius = 1000) {
  return useQuery({
    queryKey: ['nearby', { lat: coord.lat, lng: coord.lng, radius }],
    queryFn: () => fetchNearby(coord.lat, coord.lng, radius),
  });
}

/** 식당 상세 단건. */
export function usePlaceDetail(placeId: number) {
  return useQuery({
    queryKey: ['place', placeId],
    queryFn: () => fetchPlaceDetail(placeId),
  });
}
