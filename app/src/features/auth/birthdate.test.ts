import { daysInMonth, isAtLeast14, formatBirth, toIsoDate, clampDay } from './birthdate';

describe('daysInMonth', () => {
  it('윤년 2월=29, 평년 2월=28, 4월=30', () => {
    expect(daysInMonth(2024, 2)).toBe(29);
    expect(daysInMonth(2023, 2)).toBe(28);
    expect(daysInMonth(2026, 4)).toBe(30);
  });
});

describe('isAtLeast14', () => {
  const today = new Date(2026, 6, 24); // 2026-07-24 (month 0-index)
  it('정확히 14번째 생일 당일이면 true', () => {
    expect(isAtLeast14({ y: 2012, m: 7, d: 24 }, today)).toBe(true);
  });
  it('14번째 생일 하루 전이면 false', () => {
    expect(isAtLeast14({ y: 2012, m: 7, d: 25 }, today)).toBe(false);
  });
});

describe('format/iso', () => {
  it('표시·ISO 0패딩', () => {
    expect(formatBirth({ y: 1998, m: 3, d: 5 })).toBe('1998.03.05');
    expect(toIsoDate({ y: 1998, m: 3, d: 5 })).toBe('1998-03-05');
  });
});

describe('clampDay', () => {
  it('그 달 일수 초과 시 마지막 날로', () => {
    expect(clampDay({ y: 2023, m: 2, d: 31 })).toEqual({ y: 2023, m: 2, d: 28 });
    expect(clampDay({ y: 2026, m: 5, d: 15 })).toEqual({ y: 2026, m: 5, d: 15 });
  });
});
