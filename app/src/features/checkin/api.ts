import { apiGet, apiPost, apiPatch } from '@/shared/api/client';

export type CheckInStatus = 'SEEKING' | 'ACTIVE' | 'TOGETHER' | 'ENDED' | 'CANCELLED';
export type CheckIn = {
  checkInId: number; placeId: number; status: CheckInStatus;
  startedAt: string; endedAt: string | null;
  matchedAt: string | null; partnerNickname: string | null;
};
export type CheckInStats = { todayCount: number; activeCount: number; seekingCount: number };
export type MapMarker = {
  placeId: number; name: string; latitude: number; longitude: number;
  activeCount: number; seekingCount: number;
};
/** 모집중(SEEKING) 한 명 — 같이먹기 신청 대상. (구 ActiveDiner) */
export type Seeker = {
  checkInId: number; userId: number; nickname: string; startedAt: string; elapsedMinutes: number;
};

/** 내 현재 체크인(SEEKING/ACTIVE/TOGETHER, 없으면 null). */
export const fetchMyCheckIn = () => apiGet<CheckIn | null>('/check-ins/me');
/** 모집 시작 → SEEKING 생성. */
export const startCheckIn = (placeId: number) => apiPost<CheckIn>('/check-ins', { placeId });
/** 모집중 → 혼밥중(혼자 먹기 시작). */
export const dineAlone = (checkInId: number) => apiPatch<CheckIn>(`/check-ins/${checkInId}/dine-alone`);
export const endCheckIn = (checkInId: number) => apiPatch<CheckIn>(`/check-ins/${checkInId}/end`);
export const cancelCheckIn = (checkInId: number) => apiPatch<CheckIn>(`/check-ins/${checkInId}/cancel`);
export const fetchStats = () => apiGet<CheckInStats>('/check-ins/stats');
export const fetchMap = (lat: number, lng: number, radius = 1000) =>
  apiGet<MapMarker[]>(`/check-ins/map?lat=${lat}&lng=${lng}&radius=${radius}`);
/** 같은 식당 현재 모집중 목록(매칭 대상). (구 fetchActiveDiners) */
export const fetchSeekers = (placeId: number) =>
  apiGet<Seeker[]>(`/places/${placeId}/check-ins`);
