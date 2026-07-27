// 회원 탈퇴 API 호출 + 약관 목록 구성.
import { apiDelete } from '@/shared/api/client';
import { TERMS_CONTENT } from '@/features/auth/termsContent';

/** 약관 목록 화면에 보여줄 항목의 순서. 필수 3종이 먼저 오고 선택(마케팅)이 마지막이다.
 *  TERMS_CONTENT가 이 목록의 원본(문안) 소유자이므로, 여기 순서는 그 키 집합을 그대로 반영해야
 *  한다 — 새 문서가 TERMS_CONTENT에만 추가되고 이 배열이 갱신되지 않으면 그 문서는 목록에서
 *  조용히 빠진다. termsListItems()는 그 드리프트를 콘솔 경고로 남기되 예외는 던지지 않는다 —
 *  이 함수는 렌더 중에 호출되고 ErrorBoundary는 NavigationContainer 위(App.tsx)를 감싸고 있어,
 *  여기서 던지면 약관 목록 화면 하나가 아니라 앱 전체가 크래시한다. */
const TERMS_ORDER = ['service', 'privacy', 'location', 'marketing'] as const;

export type TermsListItem = { key: string; title: string };

/** 약관 4종을 정해진 순서로 돌려준다. 문안 자체는 termsContent.ts가 소유한다.
 *  TERMS_ORDER가 다루지 않는 TERMS_CONTENT 키가 있으면(드리프트) 그 문서만 목록에서 빠지고
 *  콘솔 경고를 남긴다 — 커버된 문서는 정상적으로 계속 노출된다(렌더 중 예외로 앱 전체를 죽이지 않음). */
export function termsListItems(): TermsListItem[] {
  const uncovered = Object.keys(TERMS_CONTENT).filter((key) => !(TERMS_ORDER as readonly string[]).includes(key));
  if (uncovered.length > 0) {
    console.warn(
      `termsListItems: TERMS_CONTENT에 TERMS_ORDER가 다루지 않는 키가 있습니다(${uncovered.join(', ')}) — 순서 목록을 갱신해주세요.`,
    );
  }
  return TERMS_ORDER.map((key) => ({ key, title: TERMS_CONTENT[key].title }));
}

/**
 * 회원 탈퇴. 성공하면 서버에서 개인정보가 파기되고 계정이 익명화된다(되돌릴 수 없음).
 * 호출한 쪽은 성공 직후 반드시 로그아웃해야 한다 — 남은 토큰은 다음 요청에서 401로 막힌다.
 */
export function withdrawAccount(): Promise<void> {
  return apiDelete<void>('/users/me');
}
