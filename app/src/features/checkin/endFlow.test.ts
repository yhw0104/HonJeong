import { decideEndAction, CANCEL_PROMPT_WINDOW_MIN } from './endFlow';

const base = { checkInId: 1, placeId: 10, endedAt: null, matchedAt: null, partnerNickname: null };

test('ACTIVE + 30분 미만 → prompt', () => {
  const started = new Date('2026-07-03T12:00:00Z');
  const now = new Date('2026-07-03T12:20:00Z').getTime(); // 20분 경과
  expect(decideEndAction({ ...base, status: 'ACTIVE', startedAt: started.toISOString() }, now))
    .toBe('prompt');
});

test('ACTIVE + 30분 이상 → end', () => {
  const started = new Date('2026-07-03T12:00:00Z');
  const now = new Date('2026-07-03T12:40:00Z').getTime(); // 40분
  expect(decideEndAction({ ...base, status: 'ACTIVE', startedAt: started.toISOString() }, now))
    .toBe('end');
});

test('TOGETHER는 항상 end (혼밥 아님 → 취소 프롬프트 없음)', () => {
  const started = new Date('2026-07-03T12:00:00Z');
  const now = new Date('2026-07-03T12:05:00Z').getTime();
  expect(decideEndAction({ ...base, status: 'TOGETHER', startedAt: started.toISOString() }, now))
    .toBe('end');
});

test('임계값은 30분', () => {
  expect(CANCEL_PROMPT_WINDOW_MIN).toBe(30);
});

test('ACTIVE + 경과 정확히 30분 → end (경계는 < 30이라 30분은 이미 end)', () => {
  const started = new Date('2026-07-03T12:00:00Z');
  const now = new Date('2026-07-03T12:30:00Z').getTime(); // 정확히 30분 경과
  expect(decideEndAction({ ...base, status: 'ACTIVE', startedAt: started.toISOString() }, now))
    .toBe('end');
});
