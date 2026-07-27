// 소셜 로그인 응답 → 다음 행동 결정. 화면 배선과 분리해 이 판단만 테스트한다
// (프로젝트 관례: 앱은 순수 로직만 자동 테스트).

export type OAuthResponse = {
  onboarding: boolean;
  onboardingToken?: string;
  accessToken?: string;
  refreshToken?: string;
};

export type OAuthNext =
  | { kind: 'onboarding'; onboardingToken: string }
  | { kind: 'login'; tokens: { accessToken: string; refreshToken: string } }
  | { kind: 'invalid' };

/** 서버 응답이 계약대로 왔을 때만 진행한다. 필드가 비면 'invalid'로 정직하게 드러낸다. */
export function oauthNext(result: OAuthResponse): OAuthNext {
  if (result.onboarding) {
    return result.onboardingToken
      ? { kind: 'onboarding', onboardingToken: result.onboardingToken }
      : { kind: 'invalid' };
  }
  return result.accessToken && result.refreshToken
    ? { kind: 'login', tokens: { accessToken: result.accessToken, refreshToken: result.refreshToken } }
    : { kind: 'invalid' };
}
