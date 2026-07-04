/** 거리(m)를 표시 문자열로. 1km 미만은 "120m", 이상은 "1.5km". */
export function formatDistance(meters: number): string {
  if (meters < 1000) return `${Math.round(meters)}m`;
  return `${(meters / 1000).toFixed(1)}km`;
}

/** 경과(분)를 표시 문자열로. 60분 미만은 "25분째", 이상은 "N시간째". */
export function formatElapsed(minutes: number): string {
  if (minutes < 60) return `${minutes}분째`;
  return `${Math.floor(minutes / 60)}시간째`;
}

/** 식사 성향 enum을 프로필 표시용 풀 라벨로. 미설정(null/undefined)은 null(표시 생략). */
export function diningStyleLabel(style: 'TALK' | 'QUIET' | null | undefined): string | null {
  if (style === 'TALK') return '도란도란 대화하며';
  if (style === 'QUIET') return '조용히 각자';
  return null;
}

/** 주소의 행정구역 머리(시·도 ~ 시·군·구, 지번이면 동까지)만 반환한다.
 *  도로명(로/길로 끝) 또는 번지(숫자로 시작) 토큰 바로 앞까지 자른다 — 토큰 경계라 글자 중간이 안 끊긴다.
 *  예: "서울특별시 마포구 성미산로 161-4" → "서울특별시 마포구". 뗄 게 없으면 원문 그대로. */
export function addressHead(full: string): string {
  const trimmed = (full ?? '').trim();
  const tokens = trimmed.split(/\s+/);
  const idx = tokens.findIndex((t) => /[로길]$/.test(t) || /^\d/.test(t));
  if (idx <= 0) return trimmed;
  return tokens.slice(0, idx).join(' ');
}
