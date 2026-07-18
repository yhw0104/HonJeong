import { verifyGateState } from './verifyGate';

const base = { secs: 120, resending: false, cooldown: 0, codeLen: 6, cells: 6, verifying: false };

describe('verifyGateState', () => {
  it('코드 6자 + 미만료 + 미검증 → 인증 가능', () => {
    const g = verifyGateState(base);
    expect(g.canVerify).toBe(true);
    expect(g.expired).toBe(false);
  });
  it('만료(secs 0)면 인증 불가 + expired', () => {
    const g = verifyGateState({ ...base, secs: 0 });
    expect(g.canVerify).toBe(false);
    expect(g.expired).toBe(true);
  });
  it('코드 미완성이면 인증 불가', () => {
    expect(verifyGateState({ ...base, codeLen: 3 }).canVerify).toBe(false);
  });
  it('검증 중이면 인증 불가', () => {
    expect(verifyGateState({ ...base, verifying: true }).canVerify).toBe(false);
  });
  it('쿨다운 남으면 재전송 불가 + 라벨에 초', () => {
    const g = verifyGateState({ ...base, cooldown: 12 });
    expect(g.canResend).toBe(false);
    expect(g.resendLabel).toBe('12초 후 재전송');
  });
  it('재전송 중이면 재전송 불가 + 전송 중 라벨', () => {
    const g = verifyGateState({ ...base, resending: true });
    expect(g.canResend).toBe(false);
    expect(g.resendLabel).toBe('전송 중…');
  });
  it('쿨다운 0 + 미전송이면 재전송 가능 + 기본 라벨', () => {
    const g = verifyGateState(base);
    expect(g.canResend).toBe(true);
    expect(g.resendLabel).toBe('재전송');
  });
});
