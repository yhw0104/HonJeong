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

  it('TERMS_CONTENT에 순서 목록이 다루지 않는 키가 추가되면 조용히 누락되는 대신 즉시 실패한다', () => {
    TERMS_CONTENT.__drift_test__ = { title: '드리프트 테스트', body: '' };
    try {
      expect(() => termsListItems()).toThrow();
    } finally {
      delete TERMS_CONTENT.__drift_test__;
    }
  });
});
