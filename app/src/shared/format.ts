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
/** 백엔드 DiningStyle enum 값. 프로필 응답에 이 문자열로 온다. */
export type DiningStyle = 'TALK' | 'QUIET';

/**
 * 성향별 표시 문구 — **앱 전체에서 여기 하나만 본다.**
 *
 * <p>★원래 이 문구가 네 곳에 흩어져 있었다: 선택 화면(ProfileSetup·ProfileEdit)·내 프로필·
 * 메이트 프로필이 각자 문장을 들고 있었고, 아래 diningStyleLabel이 제목만 또 들고 있었다.
 * 그래서 같은 설정이 화면마다 다르게 읽혔다 — "먹는 게 좋아요" / "먹어도 좋아요" /
 * "말 없이 각자 편안하게 드세요". 프로필에서 성향을 바꿔도 내 프로필 화면만 옛 문구를
 * 그대로 보여줬고, 그게 실기에서 지적됐다(2026-08-20). 문구를 고칠 일이 있으면 여기만 고친다.
 */
export const DINING_STYLE_LABEL: Record<DiningStyle, { title: string; sub: string }> = {
  TALK: { title: '도란도란 대화하며', sub: '가볍게 이야기 나누는 게 좋아요' },
  QUIET: { title: '조용히 각자', sub: '편하게, 말 없이 먹는 게 좋아요' },
};

/** 제목만 필요한 자리(프로필 한 줄 요약·메이트 카드 칩)용. 값이 없으면 null(표시 생략). */
export function diningStyleLabel(style: DiningStyle | null | undefined): string | null {
  return style === 'TALK' || style === 'QUIET' ? DINING_STYLE_LABEL[style].title : null;
}

/** 연령대+성별을 "20대 여성" 형태의 표시 문자열로. 없는 쪽은 생략, 둘 다 없으면 null(표시 생략). */
export function ageGenderLabel(
  ageGroup: string | null | undefined,
  gender: string | null | undefined,
): string | null {
  const genderText = gender === 'FEMALE' ? '여성' : gender === 'MALE' ? '남성' : null;
  const joined = [ageGroup, genderText].filter(Boolean).join(' ');
  return joined || null;
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

/** 발생 시각(KST naive 문자열)을 "N분 전" 상대 표기로. now는 테스트 주입용 — 화면에선 new Date(). */
export function formatTimeAgo(createdAt: string, now: Date): string {
  const diffMin = Math.floor((now.getTime() - new Date(createdAt).getTime()) / 60_000);
  if (diffMin < 1) return '방금 전';
  if (diffMin < 60) return `${diffMin}분 전`;
  const hours = Math.floor(diffMin / 60);
  if (hours < 24) return `${hours}시간 전`;
  return `${Math.floor(hours / 24)}일 전`;
}
