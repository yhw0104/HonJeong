import { noticeCategoryLabel, isNewNotice, NEW_WINDOW_DAYS } from './noticeCopy';

test('카테고리 코드 → 한글 라벨', () => {
  expect(noticeCategoryLabel('UPDATE')).toBe('업데이트');
  expect(noticeCategoryLabel('EVENT')).toBe('이벤트');
  expect(noticeCategoryLabel('GENERAL')).toBe('안내');
});

test('알 수 없는 카테고리는 그대로 노출(방어)', () => {
  expect(noticeCategoryLabel('WHATEVER')).toBe('WHATEVER');
});

test('NEW — 게시 7일 미만이면 true, 7일 이상이면 false', () => {
  const now = new Date('2026-07-08T12:00:00');
  expect(isNewNotice('2026-07-02T12:00:00', now)).toBe(true); // 6일 전
  expect(isNewNotice('2026-07-01T12:00:00', now)).toBe(false); // 정확히 7일 전(경계)
  expect(isNewNotice('2026-06-20T12:00:00', now)).toBe(false); // 18일 전
});

test('NEW — 게시 일시를 파싱할 수 없으면 false(방어)', () => {
  expect(isNewNotice('not-a-date', new Date('2026-07-08T12:00:00'))).toBe(false);
});

test('NEW 기준 상수는 7일', () => {
  expect(NEW_WINDOW_DAYS).toBe(7);
});
