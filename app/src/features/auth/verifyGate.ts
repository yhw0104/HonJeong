// 인증코드 화면의 버튼 게이팅 판정(순수). 만료 잠금 + 재전송 쿨다운/전송중 라벨.
export function verifyGateState(input: {
  secs: number;        // 남은 만료 시간(초)
  resending: boolean;  // 재전송 요청 진행 중
  cooldown: number;    // 재전송 쿨다운 남은 초
  codeLen: number;     // 입력된 자릿수
  cells: number;       // 필요한 자릿수
  verifying: boolean;  // 인증 요청 진행 중
}): { canVerify: boolean; canResend: boolean; expired: boolean; resendLabel: string } {
  const expired = input.secs <= 0;
  const canVerify = input.codeLen >= input.cells && !input.verifying && !expired;
  const canResend = !input.resending && input.cooldown <= 0;
  const resendLabel = input.resending
    ? '전송 중…'
    : input.cooldown > 0
      ? `${input.cooldown}초 후 재전송`
      : '재전송';
  return { canVerify, canResend, expired, resendLabel };
}
