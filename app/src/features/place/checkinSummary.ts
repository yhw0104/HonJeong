// 시간대 미니 바 높이 정규화(최대=1) + 버킷 라벨. 전부 0이면 전부 0(0 나눔 방어).
export const PERIOD_LABEL: Record<string, string> = {
  MORNING: '아침',
  LUNCH: '점심',
  EVENING: '저녁',
  NIGHT: '밤',
};

export function barHeights(periods: { key: string; count: number }[]): number[] {
  const max = periods.reduce((m, p) => Math.max(m, p.count), 0);
  if (max === 0) return periods.map(() => 0);
  return periods.map((p) => p.count / max);
}
