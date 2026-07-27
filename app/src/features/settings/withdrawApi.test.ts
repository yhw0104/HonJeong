import { termsListItems } from './withdrawApi';
import { TERMS_CONTENT } from '@/features/auth/termsContent';

describe('termsListItems', () => {
  it('약관 4종을 정해진 순서로 돌려준다 — 필수 3종이 먼저, 선택(마케팅)이 마지막', () => {
    expect(termsListItems().map((t) => t.key)).toEqual(['service', 'privacy', 'location', 'marketing']);
  });

  it('각 항목의 제목이 TERMS_CONTENT 원본 문안의 제목과 일치한다', () => {
    for (const item of termsListItems()) {
      expect(item.title).toBe(TERMS_CONTENT[item.key].title);
    }
  });

  it('TERMS_CONTENT에 순서 목록이 다루지 않는 키가 추가되면 던지지 않고 콘솔 경고만 남긴 채 나머지 문서는 정상 반환한다', () => {
    const warnSpy = jest.spyOn(console, 'warn').mockImplementation(() => {});
    TERMS_CONTENT.__drift_test__ = { title: '드리프트 테스트', body: '' };
    try {
      expect(() => termsListItems()).not.toThrow();
      expect(termsListItems().map((t) => t.key)).toEqual(['service', 'privacy', 'location', 'marketing']);
      expect(warnSpy).toHaveBeenCalledTimes(2); // 위 termsListItems() 두 번 호출分
    } finally {
      delete TERMS_CONTENT.__drift_test__;
      warnSpy.mockRestore();
    }
  });
});
