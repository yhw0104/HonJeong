// 뱃지 표시 정의. 서버는 판정(key·달성), 앱은 표시(아이콘·티어·이름)를 소유 — key로 결합.
import type { BadgeIconKey, BadgeTier } from './BadgeMedal';

export type BadgeKey =
  | 'SOLO_1' | 'SOLO_10' | 'SOLO_50' | 'DIARY_1' | 'DIARY_10'
  | 'TOGETHER_1' | 'MATE_1' | 'MATE_5' | 'FAV_1' | 'FAV_10';

// how = 획득 방법 한 줄(탭 시 상세 시트). icon/tier/tierNum = 메달 표시(screens/뱃지 시안.html 기반).
export type BadgeDef = {
  key: BadgeKey;
  name: string;
  how: string;
  icon: BadgeIconKey;
  tier: BadgeTier;
  tierNum?: number;
};

export const BADGE_DEFS: BadgeDef[] = [
  { key: 'SOLO_1', name: '첫 혼밥', how: '혼밥을 한 번 인증하면 획득해요', icon: 'sprout', tier: 'brand' },
  { key: 'SOLO_10', name: '혼밥 10회', how: '혼밥을 10번 인증하면 획득해요', icon: 'utensils', tier: 'brand', tierNum: 10 },
  { key: 'SOLO_50', name: '혼밥 50회', how: '혼밥을 50번 인증하면 획득해요', icon: 'trophy', tier: 'gold', tierNum: 50 },
  { key: 'DIARY_1', name: '첫 일기', how: '혼밥 일기를 한 편 쓰면 획득해요', icon: 'camera', tier: 'brand' },
  { key: 'DIARY_10', name: '일기 10편', how: '혼밥 일기를 10편 쓰면 획득해요', icon: 'bookOpen', tier: 'gold', tierNum: 10 },
  { key: 'TOGETHER_1', name: '첫 같이 먹기', how: '같이 먹기를 한 번 성사하면 획득해요', icon: 'handshake', tier: 'mint' },
  { key: 'MATE_1', name: '첫 메이트', how: '메이트를 한 명 맺으면 획득해요', icon: 'userPlus', tier: 'mint' },
  { key: 'MATE_5', name: '메이트 5명', how: '메이트를 5명 맺으면 획득해요', icon: 'users', tier: 'mint', tierNum: 5 },
  { key: 'FAV_1', name: '첫 즐겨찾기', how: '식당을 한 곳 즐겨찾기하면 획득해요', icon: 'bookmark', tier: 'brand' },
  { key: 'FAV_10', name: '단골 10곳', how: '식당을 10곳 즐겨찾기하면 획득해요', icon: 'house', tier: 'gold', tierNum: 10 },
];

// 서버 응답 한 건(GET /users/me/badges).
export type BadgeStatus = { key: string; earned: boolean; earnedAt: string | null };
export type BadgeView = {
  key: string;
  name: string;
  how: string;
  icon: BadgeIconKey;
  tier: BadgeTier;
  tierNum?: number;
  earned: boolean;
  earnedAt: string | null;
};

/** 서버 현황을 표시용으로 병합(BADGE_DEFS 순서 유지). 서버의 알 수 없는 key는 버린다(폴백). */
export function toBadgeViews(statuses: BadgeStatus[]): BadgeView[] {
  const byKey = new Map(statuses.map((s) => [s.key, s]));
  return BADGE_DEFS.map((d) => {
    const s = byKey.get(d.key);
    return {
      key: d.key,
      name: d.name,
      how: d.how,
      icon: d.icon,
      tier: d.tier,
      tierNum: d.tierNum,
      earned: s?.earned ?? false,
      earnedAt: s?.earnedAt ?? null,
    };
  });
}

/** 획득 개수(총계는 BADGE_DEFS.length). */
export function earnedCount(statuses: BadgeStatus[]): number {
  return statuses.filter((s) => s.earned).length;
}

/** 획득 뱃지를 획득 시각 최신순으로 limit개. earnedAt 없으면 뒤로. */
export function recentEarned(statuses: BadgeStatus[], limit: number): BadgeView[] {
  return toBadgeViews(statuses)
    .filter((v) => v.earned)
    .sort((a, b) => (b.earnedAt ?? '').localeCompare(a.earnedAt ?? ''))
    .slice(0, limit);
}
