import { apiGet, apiPatch } from '@/shared/api/client';

export type DiningStyle = 'TALK' | 'QUIET';

// GET /users/me 응답 중 앱에서 쓰는 필드만 타입화(나머지는 무시).
export type MyProfile = {
  nickname: string | null;
  region: string | null;
  regionLat: number | null;
  regionLng: number | null;
  introduction: string | null;
  diningStyle: DiningStyle | null;
  favoriteFoods: string[];
};

export const fetchMyProfile = () => apiGet<MyProfile>('/users/me');

/** PATCH /users/me 본문(보낸 필드만 변경). favoriteFoods는 보내면 통째로 교체. */
export type UpdateProfileBody = {
  nickname?: string;
  introduction?: string;
  diningStyle?: DiningStyle;
  favoriteFoods?: string[];
};

export const updateMyProfile = (body: UpdateProfileBody) => apiPatch<MyProfile>('/users/me', body);
