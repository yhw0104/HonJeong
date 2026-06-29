import { FAVORITE_COLORS, DEFAULT_FAVORITE_COLOR, isValidFavoriteColor } from './favoriteColors';

describe('favoriteColors', () => {
  it('기본색은 브랜드 오렌지이고 팔레트에 포함된다', () => {
    expect(DEFAULT_FAVORITE_COLOR).toBe('#FF5A1F');
    expect(FAVORITE_COLORS).toContain('#FF5A1F');
  });

  it('팔레트는 8색이고 중복이 없다', () => {
    expect(FAVORITE_COLORS).toHaveLength(8);
    expect(new Set(FAVORITE_COLORS).size).toBe(8);
  });

  it('팔레트 색만 유효하다', () => {
    expect(isValidFavoriteColor('#FF5A1F')).toBe(true);
    expect(isValidFavoriteColor('#000000')).toBe(false);
  });
});
