import { barHeights } from './checkinSummary';

describe('barHeights', () => {
  it('최대 카운트가 1.0, 나머지는 비율', () => {
    expect(barHeights([{ key: 'A', count: 4 }, { key: 'B', count: 2 }, { key: 'C', count: 0 }]))
      .toEqual([1, 0.5, 0]);
  });
  it('전부 0이면 전부 0(0 나눔 방어)', () => {
    expect(barHeights([{ key: 'A', count: 0 }, { key: 'B', count: 0 }])).toEqual([0, 0]);
  });
  it('빈 배열이면 빈 배열', () => {
    expect(barHeights([])).toEqual([]);
  });
});
