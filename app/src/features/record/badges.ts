// 뱃지 표시 정의(이모지·이름). 서버는 판정(key·달성), 앱은 표시를 소유 — key로 결합한다.
export type BadgeKey =
  | 'SOLO_1' | 'SOLO_10' | 'SOLO_50' | 'DIARY_1' | 'DIARY_10'
  | 'TOGETHER_1' | 'MATE_1' | 'MATE_5' | 'FAV_1' | 'FAV_10';

export type BadgeDef = { key: BadgeKey; emoji: string; name: string };

export const BADGE_DEFS: BadgeDef[] = [
  { key: 'SOLO_1', emoji: '🌱', name: '첫 혼밥' },
  { key: 'SOLO_10', emoji: '🍚', name: '혼밥 10회' },
  { key: 'SOLO_50', emoji: '🏆', name: '혼밥 50회' },
  { key: 'DIARY_1', emoji: '📷', name: '첫 일기' },
  { key: 'DIARY_10', emoji: '📖', name: '일기 10편' },
  { key: 'TOGETHER_1', emoji: '🤝', name: '첫 같이 먹기' },
  { key: 'MATE_1', emoji: '🫂', name: '첫 메이트' },
  { key: 'MATE_5', emoji: '👥', name: '메이트 5명' },
  { key: 'FAV_1', emoji: '🔖', name: '첫 즐겨찾기' },
  { key: 'FAV_10', emoji: '🏘️', name: '단골 10곳' },
];

// 서버 응답 한 건(GET /users/me/badges).
export type BadgeStatus = { key: string; earned: boolean; earnedAt: string | null };
export type BadgeView = { key: string; emoji: string; name: string; earned: boolean; earnedAt: string | null };

/** 서버 현황을 표시용으로 병합(BADGE_DEFS 순서 유지). 서버의 알 수 없는 key는 버린다(폴백). */
export function toBadgeViews(statuses: BadgeStatus[]): BadgeView[] {
  const byKey = new Map(statuses.map((s) => [s.key, s]));
  return BADGE_DEFS.map((d) => {
    const s = byKey.get(d.key);
    return { key: d.key, emoji: d.emoji, name: d.name, earned: s?.earned ?? false, earnedAt: s?.earnedAt ?? null };
  });
}

/** 획득 개수(총계는 BADGE_DEFS.length). */
export function earnedCount(statuses: BadgeStatus[]): number {
  return statuses.filter((s) => s.earned).length;
}

/** 획득 뱃지를 획득 시각 최신순으로 limit개(이모지 포함). earnedAt 없으면 뒤로. */
export function recentEarned(statuses: BadgeStatus[], limit: number): BadgeView[] {
  return toBadgeViews(statuses)
    .filter((v) => v.earned)
    .sort((a, b) => (b.earnedAt ?? '').localeCompare(a.earnedAt ?? ''))
    .slice(0, limit);
}
