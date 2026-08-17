import { isAppleLoginAvailable, loginWithApple } from './appleLogin';

jest.mock('expo-apple-authentication', () => ({
  signInAsync: jest.fn(),
  isAvailableAsync: jest.fn(),
  AppleAuthenticationScope: { FULL_NAME: 0, EMAIL: 1 },
}));

import * as AppleAuthentication from 'expo-apple-authentication';

const signInAsync = AppleAuthentication.signInAsync as jest.Mock;
const isAvailableAsync = AppleAuthentication.isAvailableAsync as jest.Mock;

describe('loginWithApple', () => {
  beforeEach(() => jest.clearAllMocks());

  it('성공하면 identityToken과 authorizationCode를 돌려준다', async () => {
    signInAsync.mockResolvedValue({ identityToken: 'id-token', authorizationCode: 'code' });

    await expect(loginWithApple()).resolves.toEqual({
      identityToken: 'id-token',
      authorizationCode: 'code',
    });
  });

  it('★이메일·이름을 요청하지 않는다 — 처리방침이 수집하지 않는다고 적혀 있다', async () => {
    signInAsync.mockResolvedValue({ identityToken: 'id-token', authorizationCode: 'code' });

    await loginWithApple();

    expect(signInAsync).toHaveBeenCalledWith({ requestedScopes: [] });
  });

  it('★애플이 이메일·이름을 실어 보내도 밖으로 내보내지 않는다', async () => {
    // 스코프를 비워도 최초 로그인 응답에 값이 실려 올 수 있다(기기·OS 버전에 따라).
    // 그 경우에도 우리가 만든 자격증명에는 절대 섞이지 않아야 한다 — 저장하지 않겠다고 공지한 값이다.
    signInAsync.mockResolvedValue({
      identityToken: 'id-token',
      authorizationCode: 'code',
      email: 'someone@privaterelay.appleid.com',
      fullName: { givenName: '길동', familyName: '홍' },
      user: 'apple-sub',
    });

    const credential = await loginWithApple();

    expect(Object.keys(credential ?? {}).sort()).toEqual(['authorizationCode', 'identityToken']);
  });

  it('사용자가 취소하면 에러가 아니라 null이다', async () => {
    signInAsync.mockRejectedValue({ code: 'ERR_REQUEST_CANCELED' });

    await expect(loginWithApple()).resolves.toBeNull();
  });

  it('identityToken이 없으면 실패로 던진다 — 조용히 넘어가지 않는다', async () => {
    signInAsync.mockResolvedValue({ identityToken: null, authorizationCode: 'code' });

    await expect(loginWithApple()).rejects.toThrow();
  });

  it('authorizationCode 키가 아예 없으면 undefined가 아니라 null로 채운다', async () => {
    // 키를 빼는 게 중요하다 — null을 흘려 넣으면 `?? null`을 지워도 통과해 버려서
    // 이 테스트가 아무것도 지키지 못한다. undefined만이 두 구현을 갈라놓는 입력이다.
    signInAsync.mockResolvedValue({ identityToken: 'id-token' });

    await expect(loginWithApple()).resolves.toEqual({
      identityToken: 'id-token',
      authorizationCode: null,
    });
  });

  it('code가 없어도 메시지로 취소를 알아본다 — 폴백 경로', async () => {
    signInAsync.mockRejectedValue(new Error('The user canceled the authorization attempt'));

    await expect(loginWithApple()).resolves.toBeNull();
  });

  it('취소가 아닌 실패는 그대로 던진다', async () => {
    signInAsync.mockRejectedValue(new Error('boom'));

    await expect(loginWithApple()).rejects.toThrow('boom');
  });
});

describe('isAppleLoginAvailable', () => {
  beforeEach(() => jest.clearAllMocks());

  it('SDK가 알려주는 값을 그대로 돌려준다', async () => {
    isAvailableAsync.mockResolvedValue(true);
    await expect(isAppleLoginAvailable()).resolves.toBe(true);

    isAvailableAsync.mockResolvedValue(false);
    await expect(isAppleLoginAvailable()).resolves.toBe(false);
  });

  it('확인 자체가 실패하면 던지지 않고 false다 — 버튼 노출 판단이 화면을 깨면 안 된다', async () => {
    isAvailableAsync.mockRejectedValue(new Error('네이티브 모듈 없음'));

    await expect(isAppleLoginAvailable()).resolves.toBe(false);
  });
});
