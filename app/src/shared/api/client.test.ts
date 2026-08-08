// client.test.ts — 401 자동 refresh 동작. session은 모킹(SecureStore 미접근), fetch는 시나리오별 스텁.
jest.mock('@/shared/auth/session', () => ({
  getAccessToken: jest.fn(() => 'access-1'),
  getRefreshToken: jest.fn(() => 'refresh-1'),
  setTokens: jest.fn(async () => {}),
}));

import { apiGet, apiPost, refreshSession, setOnSessionExpired, shouldAttemptRefresh, ApiError } from './client';
import * as session from '@/shared/auth/session';

type Env = { success: boolean; data?: unknown; error?: { code: string; message: string } };
const ok = (data: unknown): Env => ({ success: true, data });
const fail = (code: string): Env => ({ success: false, error: { code, message: code } });
const resp = (status: number, env: Env) => ({ ok: status >= 200 && status < 300, status, json: async () => env });

beforeEach(() => {
  jest.clearAllMocks();
  setOnSessionExpired(null);
});

describe('shouldAttemptRefresh', () => {
  it('401·세션·미재시도만 true', () => {
    expect(shouldAttemptRefresh(401, true, false)).toBe(true);
    expect(shouldAttemptRefresh(401, true, true)).toBe(false);
    expect(shouldAttemptRefresh(401, false, false)).toBe(false);
    expect(shouldAttemptRefresh(500, true, false)).toBe(false);
  });
});

describe('401 자동 refresh', () => {
  it('401→refresh→원요청 재시도 성공(호출자는 401 못 느낌)', async () => {
    (global as any).fetch = jest.fn()
      .mockResolvedValueOnce(resp(401, fail('UNAUTHORIZED')))                        // GET 원요청
      .mockResolvedValueOnce(resp(200, ok({ accessToken: 'a2', refreshToken: 'r2' }))) // /auth/refresh
      .mockResolvedValueOnce(resp(200, ok({ hello: 'world' })));                      // GET 재시도
    const data = await apiGet<{ hello: string }>('/thing');
    expect(data).toEqual({ hello: 'world' });
    expect((session.setTokens as jest.Mock)).toHaveBeenCalledWith({ accessToken: 'a2', refreshToken: 'r2' });
    expect((global as any).fetch).toHaveBeenCalledTimes(3);
  });

  it('refresh 실패→onSessionExpired 호출 + 원 401 throw', async () => {
    const expired = jest.fn();
    setOnSessionExpired(expired);
    (global as any).fetch = jest.fn()
      .mockResolvedValueOnce(resp(401, fail('UNAUTHORIZED')))            // GET
      .mockResolvedValueOnce(resp(401, fail('INVALID_REFRESH_TOKEN')));  // /auth/refresh
    await expect(apiGet('/thing')).rejects.toMatchObject({ code: 'UNAUTHORIZED', status: 401 });
    expect(expired).toHaveBeenCalledTimes(1);
  });

  it('refresh가 5xx면 세션을 유지한다 — 배포 중 재시작을 강제 로그아웃으로 오해하면 안 된다', async () => {
    // docker compose up -d --build app으로 컨테이너가 재시작되는 동안 Caddy가 502를 준다.
    // 그때 refresh 토큰은 멀쩡히 살아 있다. 만료로 취급하면 세션이 날아가고,
    // 푸시가 붙은 뒤로는 기기 FCM 토큰까지 폐기된다(onSessionExpired).
    const expired = jest.fn();
    setOnSessionExpired(expired);
    (global as any).fetch = jest.fn()
      .mockResolvedValueOnce(resp(401, fail('UNAUTHORIZED')))   // GET 원요청
      .mockResolvedValueOnce(resp(502, fail('HTTP_502')));      // /auth/refresh — 서버가 잠깐 없음
    await expect(apiGet('/thing')).rejects.toMatchObject({ code: 'UNAUTHORIZED', status: 401 });
    expect(expired).not.toHaveBeenCalled();
  });

  it('refresh가 네트워크 실패면 세션을 유지한다 — "모른다"는 "무효다"가 아니다', async () => {
    const expired = jest.fn();
    setOnSessionExpired(expired);
    (global as any).fetch = jest.fn()
      .mockResolvedValueOnce(resp(401, fail('UNAUTHORIZED')))   // GET 원요청
      .mockRejectedValueOnce(new Error('connection reset'));    // /auth/refresh — 연결 자체 실패
    await expect(apiGet('/thing')).rejects.toMatchObject({ code: 'UNAUTHORIZED', status: 401 });
    expect(expired).not.toHaveBeenCalled();
  });

  it('동시 다발 401 → /auth/refresh는 1회만(single-flight)', async () => {
    let refreshed = false;
    let refreshCalls = 0;
    (global as any).fetch = jest.fn((url: string) => {
      if (String(url).includes('/auth/refresh')) {
        refreshCalls += 1;
        refreshed = true;
        return Promise.resolve(resp(200, ok({ accessToken: 'a2', refreshToken: 'r2' })));
      }
      return Promise.resolve(refreshed ? resp(200, ok({ n: 1 })) : resp(401, fail('UNAUTHORIZED')));
    });
    const results = await Promise.all([apiGet('/a'), apiGet('/b'), apiGet('/c')]);
    expect(refreshCalls).toBe(1);
    expect(results).toEqual([{ n: 1 }, { n: 1 }, { n: 1 }]);
  });

  it('★ 동시 다발 401 → refresh가 거부당해도 onSessionExpired는 1회만', async () => {
    // single-flight라 refresh는 한 번만 도는데, 그 하나의 실패를 대기자 N명이 나눠 받는다.
    // 각자 통지하면 로그아웃 1회에 FCM 토큰 폐기(deleteToken, 서버 왕복)가 N번 돈다.
    // 앱을 포그라운드로 되돌릴 때 쿼리가 한꺼번에 뜨므로 실제로 자주 겹친다.
    const expired = jest.fn();
    setOnSessionExpired(expired);
    (global as any).fetch = jest.fn((url: string) =>
      Promise.resolve(
        String(url).includes('/auth/refresh')
          ? resp(401, fail('INVALID_REFRESH_TOKEN'))
          : resp(401, fail('UNAUTHORIZED')),
      ),
    );

    await Promise.all([
      expect(apiGet('/a')).rejects.toBeInstanceOf(ApiError),
      expect(apiGet('/b')).rejects.toBeInstanceOf(ApiError),
      expect(apiGet('/c')).rejects.toBeInstanceOf(ApiError),
    ]);

    expect(expired).toHaveBeenCalledTimes(1);
  });

  it('★ refreshSession을 직접 부르는 경로(업로드)도 같은 판정을 받는다 — 5xx는 만료가 아니다', async () => {
    // 업로드는 request()를 타지 않고 refreshSession()을 직접 부른다. 판정이 refreshSession 안에
    // 있어야 이 경로도 자동으로 같은 규칙을 따른다. 예전엔 업로드가 스스로 판단해서, 배포 중
    // 502 하나에 강제 로그아웃 + FCM 토큰 폐기가 됐다.
    const expired = jest.fn();
    setOnSessionExpired(expired);
    (global as any).fetch = jest.fn().mockResolvedValueOnce(resp(502, fail('HTTP_502')));

    await expect(refreshSession()).rejects.toBeInstanceOf(ApiError);

    expect(expired).not.toHaveBeenCalled();
  });

  it('★ refreshSession을 직접 부르는 경로도 401이면 만료 통지를 받는다', async () => {
    const expired = jest.fn();
    setOnSessionExpired(expired);
    (global as any).fetch = jest.fn().mockResolvedValueOnce(resp(401, fail('INVALID_REFRESH_TOKEN')));

    await expect(refreshSession()).rejects.toBeInstanceOf(ApiError);

    expect(expired).toHaveBeenCalledTimes(1);
  });

  it('재시도도 401 → refresh 1회만, throw(무한루프 없음)', async () => {
    let refreshCalls = 0;
    (global as any).fetch = jest.fn((url: string) => {
      if (String(url).includes('/auth/refresh')) {
        refreshCalls += 1;
        return Promise.resolve(resp(200, ok({ accessToken: 'a2', refreshToken: 'r2' })));
      }
      return Promise.resolve(resp(401, fail('UNAUTHORIZED')));
    });
    await expect(apiGet('/thing')).rejects.toBeInstanceOf(ApiError);
    expect(refreshCalls).toBe(1);
  });

  it('token:null 요청의 401은 refresh 안 함', async () => {
    const expired = jest.fn();
    setOnSessionExpired(expired);
    (global as any).fetch = jest.fn().mockResolvedValueOnce(resp(401, fail('UNAUTHORIZED')));
    await expect(apiPost('/public', {}, { token: null })).rejects.toBeInstanceOf(ApiError);
    expect((global as any).fetch).toHaveBeenCalledTimes(1);
    expect(expired).not.toHaveBeenCalled();
  });

  it('token:string(온보딩) 요청의 401은 refresh 안 함', async () => {
    const expired = jest.fn();
    setOnSessionExpired(expired);
    (global as any).fetch = jest.fn().mockResolvedValueOnce(resp(401, fail('UNAUTHORIZED')));
    await expect(apiPost('/onboard', {}, { token: 'onboarding-tok' })).rejects.toBeInstanceOf(ApiError);
    expect((global as any).fetch).toHaveBeenCalledTimes(1);
    expect(expired).not.toHaveBeenCalled();
  });
});
