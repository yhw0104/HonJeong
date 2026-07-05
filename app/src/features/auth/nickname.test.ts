import { NICKNAME_MAX, canSubmitNickname, precheckNickname } from './nickname';

// precheckNickname — 서버 확인 전 단계 판정
test('2자 미만(빈값·1자·공백 포함 1자)은 idle', () => {
  expect(precheckNickname('')).toEqual({ action: 'set', status: 'idle' });
  expect(precheckNickname('가')).toEqual({ action: 'set', status: 'idle' });
  expect(precheckNickname('  가  ')).toEqual({ action: 'set', status: 'idle' });
});

test('자음·모음 낱자만이면 invalid (서버 확인 없이 거절)', () => {
  expect(precheckNickname('ㅋㅋ')).toEqual({ action: 'set', status: 'invalid' });
  expect(precheckNickname('ㅏㅏㅏ')).toEqual({ action: 'set', status: 'invalid' });
});

test('정상 입력은 서버 중복확인 필요(check)', () => {
  expect(precheckNickname('혼밥러')).toEqual({ action: 'check' });
  expect(precheckNickname('hon12')).toEqual({ action: 'check' });
});

test('기존 닉네임과 동일하면 확인 없이 available (프로필 편집: 자기 닉네임은 중복 아님)', () => {
  expect(precheckNickname('혼밥러', '혼밥러')).toEqual({ action: 'set', status: 'available' });
  expect(precheckNickname('  혼밥러 ', '혼밥러')).toEqual({ action: 'set', status: 'available' });
});

test('기존 닉네임과 다르면 check', () => {
  expect(precheckNickname('새닉네임', '혼밥러')).toEqual({ action: 'check' });
});

// canSubmitNickname — CTA/저장 버튼 게이팅
test('2자 이상 + available일 때만 제출 가능', () => {
  expect(canSubmitNickname('혼밥러', 'available')).toBe(true);
});

test('available이 아니면 제출 불가 (1글자 idle 우회 차단)', () => {
  expect(canSubmitNickname('가', 'idle')).toBe(false);
  expect(canSubmitNickname('혼밥러', 'checking')).toBe(false);
  expect(canSubmitNickname('혼밥러', 'taken')).toBe(false);
  expect(canSubmitNickname('ㅋㅋ', 'invalid')).toBe(false);
  expect(canSubmitNickname('혼밥러', 'error')).toBe(false);
});

test('빈값·2자 미만은 상태와 무관하게 제출 불가', () => {
  expect(canSubmitNickname('', 'available')).toBe(false);
  expect(canSubmitNickname('가', 'available')).toBe(false);
});

test('닉네임 최대 길이는 10 (가입·편집 공통)', () => {
  expect(NICKNAME_MAX).toBe(10);
});
