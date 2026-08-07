import { shouldPromptPush } from './prompt';

describe('shouldPromptPush', () => {
  it('처음 보는 사용자에게는 안내를 띄운다', () => {
    expect(shouldPromptPush({ seen: false, granted: false })).toBe(true);
  });

  it('이미 안내를 본 사람에게는 다시 띄우지 않는다', () => {
    expect(shouldPromptPush({ seen: true, granted: false })).toBe(false);
  });

  it('이미 권한을 준 사람에게는 띄우지 않는다 — 안내를 본 적 없어도', () => {
    expect(shouldPromptPush({ seen: false, granted: true })).toBe(false);
  });

  it('안내도 봤고 권한도 있으면 띄우지 않는다', () => {
    expect(shouldPromptPush({ seen: true, granted: true })).toBe(false);
  });
});
