import { REPORT_REASONS, reasonLabel, reportStatusLabel, reportTypeLabel, formatDotDate } from './reportCopy';

test('신고 대상 타입 칩 라벨 — 유저는 메이트, 리뷰는 리뷰', () => {
  expect(reportTypeLabel('USER')).toBe('메이트');
  expect(reportTypeLabel('REVIEW')).toBe('리뷰');
});

test('신고 사유는 6종 고정, 순서 유지', () => {
  expect(REPORT_REASONS.map((r) => r.code)).toEqual([
    'INAPPROPRIATE_MESSAGE', 'ABUSE', 'SPAM', 'FALSE_PROFILE', 'NO_SHOW', 'OTHER',
  ]);
});

test('사유 코드 → 한글 라벨', () => {
  expect(reasonLabel('SPAM')).toBe('광고 / 스팸');
  expect(reasonLabel('OTHER')).toBe('기타');
});

test('알 수 없는 사유 코드는 그대로 노출(방어)', () => {
  expect(reasonLabel('WHATEVER')).toBe('WHATEVER');
});

test('신고 상태 라벨 — 현재는 전부 접수됨, 예약 상태도 매핑', () => {
  expect(reportStatusLabel('RECEIVED')).toBe('접수됨');
  expect(reportStatusLabel('REVIEWING')).toBe('검토 중');
  expect(reportStatusLabel('RESOLVED')).toBe('처리 완료');
});

test('ISO 일시 → 2026.05.28 표기', () => {
  expect(formatDotDate('2026-05-28T12:34:56')).toBe('2026.05.28');
});

test('잘못된 일시는 빈 문자열(방어)', () => {
  expect(formatDotDate('not-a-date')).toBe('');
});
