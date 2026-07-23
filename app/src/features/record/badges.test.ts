import { toBadgeViews, earnedCount, recentEarned, BADGE_DEFS } from './badges';

const S = (key: string, earned: boolean, earnedAt: string | null = null) => ({ key, earned, earnedAt });

describe('toBadgeViews', () => {
  it('BADGE_DEFS 순서·이모지 결합, 미포함 key는 미획득', () => {
    const views = toBadgeViews([S('SOLO_1', true)]);
    expect(views).toHaveLength(10);
    expect(views[0]).toMatchObject({ key: 'SOLO_1', emoji: '🌱', name: '첫 혼밥', earned: true });
    expect(views.find((v) => v.key === 'SOLO_50')!.earned).toBe(false);
  });
  it('알 수 없는 서버 key는 무시(폴백)', () => {
    const views = toBadgeViews([S('UNKNOWN', true)]);
    expect(views.every((v) => BADGE_DEFS.some((d) => d.key === v.key))).toBe(true);
  });
});

describe('earnedCount', () => {
  it('획득한 것만 센다', () => {
    expect(earnedCount([S('SOLO_1', true), S('SOLO_10', false), S('FAV_1', true)])).toBe(2);
  });
});

describe('recentEarned', () => {
  it('earnedAt 최신순·limit·획득만', () => {
    const r = recentEarned(
      [S('SOLO_1', true, '2026-07-01T00:00:00'), S('MATE_1', true, '2026-07-20T00:00:00'), S('FAV_1', false)],
      1,
    );
    expect(r).toHaveLength(1);
    expect(r[0].key).toBe('MATE_1');
  });
});
