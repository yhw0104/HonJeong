import { apiGet } from '@/shared/api/client';

// GET /users/me 응답 중 위치 폴백에 필요한 부분만 타입화(나머지 필드는 무시).
export type MyProfile = {
  nickname: string | null;
  region: string | null;
  regionLat: number | null;
  regionLng: number | null;
};

export const fetchMyProfile = () => apiGet<MyProfile>('/users/me');
