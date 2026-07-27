import { termsListItems } from './withdrawApi';

describe('termsListItems', () => {
  it('약관 4종을 정해진 순서로 돌려준다 — 필수 3종이 먼저, 선택(마케팅)이 마지막', () => {
    expect(termsListItems().map((t) => t.key)).toEqual(['service', 'privacy', 'location', 'marketing']);
  });

  it('각 항목이 화면에 쓸 제목을 갖는다', () => {
    for (const item of termsListItems()) {
      expect(item.title.length).toBeGreaterThan(0);
    }
  });
});
