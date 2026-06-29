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

const ADDRESS_PROVINCES = [
  '서울특별시', '부산광역시', '대구광역시', '인천광역시', '광주광역시', '대전광역시', '울산광역시', '세종특별자치시',
  '경기도', '강원도', '강원특별자치도', '충청북도', '충청남도', '전라북도', '전라남도', '전북특별자치도',
  '경상북도', '경상남도', '제주특별자치도', '제주도',
  '서울', '부산', '대구', '인천', '광주', '대전', '울산', '세종',
  '경기', '강원', '충북', '충남', '전북', '전남', '경북', '경남', '제주',
];

/** 주소에서 맨 앞 시·도(광역) 토큰만 떼어 깔끔하게 줄인다. 토큰 경계로 자르므로 글자 중간이 끊기지 않는다.
 *  예: "서울특별시 마포구 성미산로 161-4" → "마포구 성미산로 161-4". 시·도가 없으면 원문 그대로. */
export function shortAddress(full: string): string {
  const trimmed = (full ?? '').trim();
  const tokens = trimmed.split(/\s+/);
  if (tokens.length > 1 && ADDRESS_PROVINCES.includes(tokens[0])) {
    return tokens.slice(1).join(' ');
  }
  return trimmed;
}
