// client.test.ts — 401 자동 refresh 동작. session은 모킹(SecureStore 미접근), fetch는 시나리오별 스텁.
jest.mock('@/shared/auth/session', () => ({
  getAccessToken: jest.fn(() => 'access-1'),
  getRefreshToken: jest.fn(() => 'refresh-1'),
  setTokens: jest.fn(async () => {}),
}));

import { apiGet, apiPost, setOnSessionExpired, shouldAttemptRefresh, ApiError } from './client';
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
