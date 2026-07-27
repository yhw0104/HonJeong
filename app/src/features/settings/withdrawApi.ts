// 회원 탈퇴 API 호출 + 약관 목록 구성.
import { apiDelete } from '@/shared/api/client';
import { TERMS_CONTENT } from '@/features/auth/termsContent';

/** 약관 목록 화면에 보여줄 항목. 필수 3종이 먼저 오고 선택(마케팅)이 마지막이다. */
const TERMS_ORDER = ['service', 'privacy', 'location', 'marketing'] as const;

export type TermsListItem = { key: string; title: string };

/** 약관 4종을 정해진 순서로 돌려준다. 문안 자체는 termsContent.ts가 소유한다. */
export function termsListItems(): TermsListItem[] {
  return TERMS_ORDER.map((key) => ({ key, title: TERMS_CONTENT[key].title }));
}

/**
 * 회원 탈퇴. 성공하면 서버에서 개인정보가 파기되고 계정이 익명화된다(되돌릴 수 없음).
 * 호출한 쪽은 성공 직후 반드시 로그아웃해야 한다 — 남은 토큰은 다음 요청에서 401로 막힌다.
 */
export function withdrawAccount(): Promise<void> {
  return apiDelete<void>('/users/me');
}
