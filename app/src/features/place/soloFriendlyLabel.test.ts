import { soloFriendlyLabel } from './soloFriendlyLabel';

describe('soloFriendlyLabel', () => {
  it('리뷰 0이면 평가 없음', () => expect(soloFriendlyLabel(null, 0)).toBe('아직 평가가 없어요'));
  it('점수 있어도 리뷰 0이면 평가 없음', () => expect(soloFriendlyLabel(4.8, 0)).toBe('아직 평가가 없어요'));
  it('rating null이면 평가 없음', () => expect(soloFriendlyLabel(null, 3)).toBe('아직 평가가 없어요'));
  it('4.5 이상은 아주 좋아요', () => expect(soloFriendlyLabel(4.5, 3)).toBe('혼밥하기 아주 좋아요'));
  it('3.5~4.4는 좋아요', () => expect(soloFriendlyLabel(3.5, 2)).toBe('혼밥하기 좋아요'));
  it('2.5~3.4는 무난해요', () => expect(soloFriendlyLabel(2.5, 1)).toBe('혼밥하기 무난해요'));
  it('2.5 미만은 아쉬워요', () => expect(soloFriendlyLabel(2.0, 1)).toBe('혼밥은 조금 아쉬워요'));
});
