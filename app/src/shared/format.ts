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
