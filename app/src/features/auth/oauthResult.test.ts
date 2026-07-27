import { oauthNext } from './oauthResult';

describe('oauthNext', () => {
  it('신규 회원(onboarding=true)이면 온보딩 토큰과 함께 온보딩으로 보낸다', () => {
    expect(oauthNext({ onboarding: true, onboardingToken: 'ONB' })).toEqual({
      kind: 'onboarding',
      onboardingToken: 'ONB',
    });
  });

  it('기존 회원이면 access/refresh로 바로 로그인시킨다', () => {
    expect(oauthNext({ onboarding: false, accessToken: 'A', refreshToken: 'R' })).toEqual({
      kind: 'login',
      tokens: { accessToken: 'A', refreshToken: 'R' },
    });
  });

  it('onboarding=true인데 토큰이 없으면 잘못된 응답으로 본다', () => {
    expect(oauthNext({ onboarding: true })).toEqual({ kind: 'invalid' });
  });

  it('onboarding=false인데 토큰이 없으면 잘못된 응답으로 본다', () => {
    expect(oauthNext({ onboarding: false, accessToken: 'A' })).toEqual({ kind: 'invalid' });
  });
});
