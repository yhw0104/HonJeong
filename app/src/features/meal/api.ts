import { apiGet, apiPost, apiPatch } from '@/shared/api/client';

export type MealRequestStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'EXPIRED';

export type MealRequestListItem = {
  mealRequestId: number;
  fromUser: { userId: number; nickname: string };
  toUser: { userId: number; nickname: string };
  placeId: number;
  placeName: string;
  message: string | null;
  status: MealRequestStatus;
  createdAt: string;
};

export type CreateMealRequestResult = {
  mealRequestId: number;
  toCheckInId: number;
  message: string | null;
  status: string;
};

export type MealRequestStatusResult = {
  mealRequestId: number;
  status: string;
  respondedAt: string;
};

/** 같이먹기 신청. place는 서버가 대상 체크인에서 파생한다. */
export const createMealRequest = (toCheckInId: number, message?: string) =>
  apiPost<CreateMealRequestResult>('/meal-requests', { toCheckInId, message });

/** 받은/보낸 신청 목록(createdAt 내림차순). */
export const listMealRequests = (role: 'received' | 'sent', status?: string) =>
  apiGet<MealRequestListItem[]>(`/meal-requests?role=${role}${status ? `&status=${status}` : ''}`);

export const acceptMealRequest = (id: number) =>
  apiPatch<MealRequestStatusResult>(`/meal-requests/${id}/accept`);

export const declineMealRequest = (id: number) =>
  apiPatch<MealRequestStatusResult>(`/meal-requests/${id}/decline`);
