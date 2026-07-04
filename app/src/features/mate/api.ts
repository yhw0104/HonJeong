import { apiGet, apiPost, apiPatch, apiDelete } from '@/shared/api/client';

export type MateRequestStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'CANCELED';
export type RelationStatus = 'NONE' | 'PENDING_SENT' | 'PENDING_RECEIVED';

export type Mate = {
  mateUserId: number;
  nickname: string | null;
  profileImageUrl: string | null;
  diningStyle: 'TALK' | 'QUIET' | null;
  region: string | null;
  isOnline: boolean;
  currentPlaceId: number | null;
  currentPlaceName: string | null;
  checkInStartedAt: string | null;
  checkInCount: number;
  mealsTogether: number;
  matesSince: string;
};

export type MateUserRef = { userId: number; nickname: string | null; profileImageUrl: string | null };

export type MateRequestListItem = {
  mateRequestId: number;
  fromUser: MateUserRef;
  toUser: MateUserRef;
  status: MateRequestStatus;
  createdAt: string;
};

export type UserSearchItem = {
  userId: number;
  nickname: string | null;
  profileImageUrl: string | null;
  region: string | null;
  diningStyle: 'TALK' | 'QUIET' | null;
  isMate: boolean;
  requestStatus: RelationStatus;
};

export type PublicProfile = {
  userId: number;
  nickname: string | null;
  profileImageUrl: string | null;
  introduction: string | null;
  region: string | null;
  gender: string | null;
  ageGroup: string | null;
  diningStyle: 'TALK' | 'QUIET' | null;
  preferredFoods: string[];
  checkInCount: number;
  mealsTogether: number;
  badgeCount: number;
  isOnline: boolean;
  currentPlaceName: string | null;
  currentPlaceId: number | null;
  isMate: boolean;
  requestStatus: RelationStatus;
};

export const fetchMyMates = () => apiGet<Mate[]>('/mates');
export const deleteMate = (mateUserId: number) => apiDelete<{ success: boolean }>(`/mates/${mateUserId}`);

export const searchUsers = (nickname: string) =>
  apiGet<UserSearchItem[]>(`/users/search?nickname=${encodeURIComponent(nickname)}`);
export const fetchPublicProfile = (userId: number) => apiGet<PublicProfile>(`/users/${userId}/profile`);

export const listMateRequests = (role: 'received' | 'sent', status?: string) =>
  apiGet<MateRequestListItem[]>(`/mate-requests?role=${role}${status ? `&status=${status}` : ''}`);
export const sendMateRequest = (toUserId: number) =>
  apiPost<{ mateRequestId: number; toUserId: number; status: string }>('/mate-requests', { toUserId });
export const acceptMateRequest = (id: number) =>
  apiPatch<{ mateRequestId: number; status: string; respondedAt: string }>(`/mate-requests/${id}/accept`);
export const declineMateRequest = (id: number) =>
  apiPatch<{ mateRequestId: number; status: string; respondedAt: string }>(`/mate-requests/${id}/decline`);
export const cancelMateRequest = (id: number) =>
  apiPatch<{ mateRequestId: number; status: string; respondedAt: string }>(`/mate-requests/${id}/cancel`);
