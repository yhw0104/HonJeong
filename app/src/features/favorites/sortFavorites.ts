// 즐겨찾기 목록 정렬 — 화면(Favorites)이 쓰는 순수 로직.

export type FavoriteSort = 'registered' | 'name';

export const FAVORITE_SORTS = [
  { key: 'registered' as const, label: '등록순' },
  { key: 'name' as const, label: '이름순' },
];

/** 등록순은 API가 주는 순서 그대로, 이름순은 가나다순(원본 불변). */
export function sortByMode<T extends { name: string }>(items: T[], mode: FavoriteSort): T[] {
  if (mode === 'registered') return items;
  return [...items].sort((a, b) => a.name.localeCompare(b.name, 'ko'));
}
