// 생년월일 순수 헬퍼 — 피커 표시/검증/직렬화. UI 상태와 분리해 테스트 가능하게 둔다.
export type Birth = { y: number; m: number; d: number };

const pad = (n: number) => String(n).padStart(2, '0');

/** 해당 연·월의 일수(윤년 반영). m은 1~12. */
export function daysInMonth(y: number, m: number): number {
  return new Date(y, m, 0).getDate();
}

/** 만 나이 기준 14세 이상인지(가입 게이트). today는 테스트 주입용. */
export function isAtLeast14(b: Birth, today: Date): boolean {
  const fourteenth = new Date(b.y + 14, b.m - 1, b.d);
  const t = new Date(today.getFullYear(), today.getMonth(), today.getDate());
  return fourteenth.getTime() <= t.getTime();
}

/** 표시용 "1998.03.05". */
export function formatBirth(b: Birth): string {
  return `${b.y}.${pad(b.m)}.${pad(b.d)}`;
}

/** 서버 전송용 ISO "1998-03-05". */
export function toIsoDate(b: Birth): string {
  return `${b.y}-${pad(b.m)}-${pad(b.d)}`;
}

/** 월/연 변경 시 일자가 그 달 일수를 넘으면 마지막 날로 클램프. */
export function clampDay(b: Birth): Birth {
  const max = daysInMonth(b.y, b.m);
  return b.d > max ? { ...b, d: max } : b;
}
