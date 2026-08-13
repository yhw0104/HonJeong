import { apiGet, apiPatch } from '@/shared/api/client';

export type DiningStyle = 'TALK' | 'QUIET';

// GET /users/me 응답 중 앱에서 쓰는 필드만 타입화(나머지는 무시).
export type MyProfile = {
  // 서버 UserProfileResponse의 첫 필드(Long id) — 소켓 이벤트의 안읽음·읽음 귀속 판정에 필요해 추가.
  id: number;
  nickname: string | null;
  profileImageUrl: string | null;
  region: string | null;
  regionLat: number | null;
  regionLng: number | null;
  introduction: string | null;
  diningStyle: DiningStyle | null;
  gender: 'FEMALE' | 'MALE' | null;
  ageGroup: string | null;
  favoriteFoods: string[];
};

export const fetchMyProfile = () => apiGet<MyProfile>('/users/me');

/** PATCH /users/me 본문(보낸 필드만 변경). favoriteFoods는 보내면 통째로 교체. */
export type UpdateProfileBody = {
  nickname?: string;
  profileImageUrl?: string;
  introduction?: string;
  diningStyle?: DiningStyle;
  favoriteFoods?: string[];
};

export const updateMyProfile = (body: UpdateProfileBody) => apiPatch<MyProfile>('/users/me', body);

// GET /users/me/activity-summary — 프로필 카드 통계(혼밥·같이먹음·즐겨찾기·메이트 카운트).
export type ActivitySummary = {
  checkInCount: number;
  reviewCount: number;
  favoriteCount: number;
  mateCount: number;
  togetherCount: number;
};

export const fetchActivitySummary = () => apiGet<ActivitySummary>('/users/me/activity-summary');
