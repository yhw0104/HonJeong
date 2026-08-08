// FileSystem.uploadAsync·session·client는 모킹(네이티브·네트워크 미접근). expo-image-picker는 jest-expo 기본 처리.
jest.mock('expo-file-system/legacy', () => ({
  FileSystemUploadType: { MULTIPART: 'MULTIPART' },
  uploadAsync: jest.fn(),
}));
jest.mock('@/shared/auth/session', () => ({
  getAccessToken: jest.fn(() => 'access-1'),
}));
jest.mock('@/shared/api/client', () => ({
  refreshSession: jest.fn(async () => {}),
  notifySessionExpired: jest.fn(),
}));

import { extractUploadedUrl, remainingSlots, uploadImages } from './imageUpload';
import * as FileSystem from 'expo-file-system/legacy';
import * as client from '@/shared/api/client';

describe('remainingSlots', () => {
  it('남은 슬롯 = max - current, 음수는 0', () => {
    expect(remainingSlots(2, 5)).toBe(3);
    expect(remainingSlots(5, 5)).toBe(0);
    expect(remainingSlots(7, 5)).toBe(0);
  });
});

describe('extractUploadedUrl', () => {
  it('files 응답 엔벨로프에서 url을 꺼낸다', () => {
    expect(extractUploadedUrl({ success: true, data: { url: 'http://x/a.jpg' } })).toBe('http://x/a.jpg');
  });
  it('url이 없으면 throw', () => {
    expect(() => extractUploadedUrl({ success: true, data: {} as any })).toThrow();
  });
});

// ── 세션 토큰 업로드 401 → 자동 refresh + 1회 재시도 (client.ts request()와 동일 정책) ──
const uploadAsync = FileSystem.uploadAsync as jest.Mock;
const refreshSession = client.refreshSession as jest.Mock;
const notifySessionExpired = client.notifySessionExpired as jest.Mock;

const res = (status: number, body: unknown) => ({ status, body: JSON.stringify(body) });
const ok = (url: string) => ({ success: true, data: { url } });
const fail = () => ({ success: false });

describe('uploadImages 401 자동 refresh', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    refreshSession.mockResolvedValue(undefined);
  });

  it('정상 200 → url 반환(refresh 안 함)', async () => {
    uploadAsync.mockResolvedValueOnce(res(200, ok('http://img/1.jpg')));
    await expect(uploadImages(['file://a'])).resolves.toEqual(['http://img/1.jpg']);
    expect(refreshSession).not.toHaveBeenCalled();
  });

  it('세션 업로드 401 → refresh → 재시도 성공(호출자는 401 못 느낌)', async () => {
    uploadAsync
      .mockResolvedValueOnce(res(401, fail()))
      .mockResolvedValueOnce(res(200, ok('http://img/1.jpg')));
    await expect(uploadImages(['file://a'])).resolves.toEqual(['http://img/1.jpg']);
    expect(refreshSession).toHaveBeenCalledTimes(1);
    expect(uploadAsync).toHaveBeenCalledTimes(2);
    expect(notifySessionExpired).not.toHaveBeenCalled();
  });

  it('★ refresh 실패 → throw하되 만료 통지는 스스로 하지 않는다', async () => {
    // 예전엔 여기서 어떤 실패든 notifySessionExpired()를 불렀다. 그래서 배포로 컨테이너가
    // 재시작되는 동안 사진을 올리면 refresh가 502를 받고 → 강제 로그아웃 + FCM 토큰 폐기가 됐다.
    // (request() 경로는 08-07 d65902b로 고쳤는데 업로드 경로만 빠져 있었다.)
    // 이제 "401만 만료"라는 판정은 refreshSession 한 곳에 있고, 여기는 실패를 흘려보내기만 한다.
    uploadAsync.mockResolvedValueOnce(res(401, fail()));
    refreshSession.mockRejectedValueOnce(new Error('refresh fail'));
    await expect(uploadImages(['file://a'])).rejects.toThrow();
    expect(notifySessionExpired).not.toHaveBeenCalled();
  });

  it('온보딩 토큰(명시 인자) 401 → refresh 안 함', async () => {
    uploadAsync.mockResolvedValueOnce(res(401, fail()));
    await expect(uploadImages(['file://a'], 'onboarding-tok')).rejects.toThrow();
    expect(refreshSession).not.toHaveBeenCalled();
    expect(notifySessionExpired).not.toHaveBeenCalled();
  });

  it('재시도도 401 → refresh 1회만, throw(무한루프 없음)', async () => {
    uploadAsync
      .mockResolvedValueOnce(res(401, fail()))
      .mockResolvedValueOnce(res(401, fail()));
    await expect(uploadImages(['file://a'])).rejects.toThrow();
    expect(refreshSession).toHaveBeenCalledTimes(1);
    expect(uploadAsync).toHaveBeenCalledTimes(2);
  });
});
