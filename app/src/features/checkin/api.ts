import { apiGet, apiPost, apiPatch } from '@/shared/api/client';

export type CheckInStatus = 'ACTIVE' | 'TOGETHER' | 'ENDED' | 'CANCELLED';
export type CheckIn = {
  checkInId: number; placeId: number; status: CheckInStatus;
  startedAt: string; endedAt: string | null;
  matchedAt: string | null; partnerNickname: string | null;
};
export type CheckInStats = { todayCount: number; activeCount: number };
export type MapMarker = {
  placeId: number; name: string; latitude: number; longitude: number; activeCount: number;
};
export type ActiveDiner = {
  checkInId: number; userId: number; nickname: string; startedAt: string; elapsedMinutes: number;
};

/** 내 현재 ACTIVE 체크인(없으면 null). */
export const fetchMyCheckIn = () => apiGet<CheckIn | null>('/check-ins/me');
export const startCheckIn = (placeId: number) => apiPost<CheckIn>('/check-ins', { placeId });
export const endCheckIn = (checkInId: number) => apiPatch<CheckIn>(`/check-ins/${checkInId}/end`);
export const cancelCheckIn = (checkInId: number) => apiPatch<CheckIn>(`/check-ins/${checkInId}/cancel`);
export const fetchStats = () => apiGet<CheckInStats>('/check-ins/stats');
export const fetchMap = (lat: number, lng: number, radius = 1000) =>
  apiGet<MapMarker[]>(`/check-ins/map?lat=${lat}&lng=${lng}&radius=${radius}`);
export const fetchActiveDiners = (placeId: number) =>
  apiGet<ActiveDiner[]>(`/places/${placeId}/check-ins`);
