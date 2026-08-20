import { apiGet } from '@/shared/api/client';
import type { Page } from '@/shared/api/types';

export type PlaceSearchItem = {
  placeId: number; name: string; category: string | null;
  address: string | null; roadAddress: string | null;
  latitude: number; longitude: number; phone: string | null;
  /**
   * 내 위치로부터의 거리(m). 좌표를 보내지 않은 검색이면 null이다.
   * ★0이 아니라 null인 이유: "0m"와 "거리를 모른다"는 다른 사실이고, 0으로 오면
   *   화면이 모든 결과를 "0m"로 표시하게 된다.
   */
  distanceMeters: number | null;
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

/**
 * 식당 이름 검색.
 *
 * <p>coord를 주면 서버가 <b>내 위치 기준 거리순</b>으로 돌려준다(반경 밖은 제외하되, 반경 안에
 * 하나도 없으면 전국 이름순으로 떨어진다 — 멀리 있는 가게를 이름으로 찾는 길은 열어 둔다).
 * coord가 없으면 예전처럼 전국 이름순이다.
 */
export const searchPlaces = (query: string, coord?: { lat: number; lng: number } | null, page = 0, size = 20) => {
  const where = coord ? `&lat=${coord.lat}&lng=${coord.lng}` : '';
  return apiGet<Page<PlaceSearchItem>>(
    `/places/search?query=${encodeURIComponent(query)}${where}&page=${page}&size=${size}`,
  );
};

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

export type MateAtPlace = {
  userId: number;
  nickname: string;
  /** 지금 이 식당에서 같이 먹을 사람 모집중(SEEKING)인지 — 신청 받을 수 있는 상태만 true. */
  seekingNow: boolean;
  soloFriendlyRating: number | null;
  reviewContent: string | null;
  togetherCount: number;
  visitCount: number;
  lastVisitedAt: string | null;
  profileImageUrl: string | null;
};
export type SavedMate = { userId: number; nickname: string; profileImageUrl: string | null };
export type PlaceMates = {
  visitedCount: number;
  mates: MateAtPlace[];
  savedCount: number;
  savedMateCount: number;
  savedMates: SavedMate[];
};

export const fetchPlaceMates = (placeId: number) =>
  apiGet<PlaceMates>(`/places/${placeId}/mates`);
