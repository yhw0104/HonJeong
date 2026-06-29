// 즐겨찾기 그룹 별표 색 팔레트. 백엔드는 길이만 검증하고, 선택은 이 8색으로 제한한다.
export const DEFAULT_FAVORITE_COLOR = '#FF5A1F';

export const FAVORITE_COLORS: string[] = [
  '#FF5A1F', // 브랜드(기본)
  '#22A65A',
  '#2F80ED',
  '#9B51E0',
  '#EB5757',
  '#F2994A',
  '#F2C94C',
  '#EB6FB0',
];

export function isValidFavoriteColor(color: string): boolean {
  return FAVORITE_COLORS.includes(color);
}
