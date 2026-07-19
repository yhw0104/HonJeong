import { apiGet } from '@/shared/api/client';
import type { Page } from '@/shared/api/types';

export type PlaceSearchItem = {
  placeId: number; name: string; category: string | null;
  address: string | null; roadAddress: string | null;
  latitude: number; longitude: number; phone: string | null;
};

export type PlaceNearbyItem = {
  placeId: number; name: string; category: string | null;
  roadAddress: string | null; latitude: number; longitude: number;
  distanceMeters: number; activeCount: number; seekingCount: number;
  photoUrls: string[]; // 대표 사진(리뷰 사진 최신순 최대 N장). 없으면 빈 배열
  reviewCount: number; // 리뷰 수(없으면 0)
  avgTasteRating: number | null; // 맛 별점 평균(소수1자리). 리뷰 없으면 null
  avgSoloFriendlyRating: number | null; // 혼밥 적합도 별점 평균(소수1자리). 리뷰 없으면 null
};

export type PlaceDetail = {
  placeId: number; name: string; category: string | null;
  address: string | null; roadAddress: string | null;
  latitude: number; longitude: number; phone: string | null; businessStatus: string;
};

export const searchPlaces = (query: string, page = 0, size = 20) =>
  apiGet<Page<PlaceSearchItem>>(
    `/places/search?query=${encodeURIComponent(query)}&page=${page}&size=${size}`,
  );

export const fetchNearby = (lat: number, lng: number, radius = 1000, page = 0, size = 20) =>
  apiGet<Page<PlaceNearbyItem>>(
    `/places/nearby?lat=${lat}&lng=${lng}&radius=${radius}&page=${page}&size=${size}`,
  );

export const fetchPlaceDetail = (placeId: number) =>
  apiGet<PlaceDetail>(`/places/${placeId}`);

export type PlaceCheckinSummary = {
  totalDiners: number;
  periods: { key: string; count: number }[];
  peakPeriodKey: string | null;
};

/** 식당 사회적 증거(누적 혼밥러 + 붐비는 시간대). */
export const fetchPlaceCheckinSummary = (placeId: number) =>
  apiGet<PlaceCheckinSummary>(`/places/${placeId}/checkin-summary`);
