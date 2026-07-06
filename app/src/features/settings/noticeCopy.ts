// 공지사항 표시 문구 — 화면(Notices)이 쓰는 순수 로직.

/** NEW 뱃지 기준(일) — 게시 후 이 일수 미만이면 NEW. */
export const NEW_WINDOW_DAYS = 7;

/** 카테고리 코드 → 화면 라벨. 알 수 없는 코드는 그대로(방어). */
export function noticeCategoryLabel(category: string): string {
  if (category === 'UPDATE') return '업데이트';
  if (category === 'EVENT') return '이벤트';
  if (category === 'GENERAL') return '안내';
  return category;
}

/** 게시 7일 미만이면 NEW. 게시 일시 파싱 불가면 false. */
export function isNewNotice(publishedAt: string, now: Date): boolean {
  const t = new Date(publishedAt).getTime();
  if (Number.isNaN(t)) return false;
  return now.getTime() - t < NEW_WINDOW_DAYS * 24 * 60 * 60 * 1000;
}
