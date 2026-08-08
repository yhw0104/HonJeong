// installation.ts — 설치 ID 생성·보관. SecureStore는 모킹(네이티브 미접근).
jest.mock('expo-secure-store', () => ({
  getItemAsync: jest.fn(),
  setItemAsync: jest.fn(async () => {}),
}));

import * as SecureStore from 'expo-secure-store';

import {
  getInstallationId,
  MAX_INSTALLATION_ID_LENGTH,
  newInstallationId,
  resetInstallationIdCacheForTest,
} from './installation';

const getItemAsync = SecureStore.getItemAsync as jest.Mock;
const setItemAsync = SecureStore.setItemAsync as jest.Mock;

beforeEach(() => {
  jest.clearAllMocks();
  resetInstallationIdCacheForTest();
});

describe('newInstallationId', () => {
  it('서버 컬럼(VARCHAR 64) 안에 들어간다', () => {
    const id = newInstallationId(Math.random, Date.now());
    expect(id.length).toBeLessThanOrEqual(MAX_INSTALLATION_ID_LENGTH);
  });

  it('소문자 영숫자만 쓴다 — 본문 JSON·로그·DB에서 이스케이프가 필요 없게', () => {
    expect(newInstallationId(Math.random, Date.now())).toMatch(/^[0-9a-z]+$/);
  });

  it('★ 같은 시각이어도 난수가 다르면 값이 갈린다 — 시각만으로 추측되면 안 된다', () => {
    // 설치 ID를 알아낸 사람은 그것을 자기 등록에 실어 상대 기기의 토큰 행을 지울 수 있다.
    // 그래서 "같은 초에 설치한 기기"끼리 값이 겹치거나 유추되면 안 된다.
    const fixedNow = 1_754_600_000_000;
    let seed = 0;
    const seededRandom = () => ((seed = (seed * 1103515245 + 12345) % 2147483648) / 2147483648);
    const a = newInstallationId(seededRandom, fixedNow);
    const b = newInstallationId(seededRandom, fixedNow);

    expect(a).not.toEqual(b);
  });

  it('난수를 여러 조각 쓴다 — 조각 하나가 0이어도 나머지가 남는다', () => {
    const id = newInstallationId(() => 0, 0);
    // now=0, 난수 전부 0이면 모든 조각이 '0'이다. 조각 수만큼 자리가 나온다(시각 1 + 난수 6).
    expect(id).toHaveLength(7);
  });
});

describe('getInstallationId', () => {
  it('저장된 값이 있으면 그대로 쓴다 — 앱을 껐다 켜도 같은 기기로 인식돼야 한다', async () => {
    getItemAsync.mockResolvedValueOnce('saved-id');

    await expect(getInstallationId()).resolves.toBe('saved-id');
    expect(setItemAsync).not.toHaveBeenCalled();
  });

  it('없으면 만들어 저장한다', async () => {
    getItemAsync.mockResolvedValueOnce(null);

    const id = await getInstallationId();

    expect(id).toMatch(/^[0-9a-z]+$/);
    expect(setItemAsync).toHaveBeenCalledWith('push.installationId', id);
  });

  it('두 번째 호출은 저장소를 다시 읽지 않는다 — 등록은 앱 시작·로그인·토큰갱신에서 여러 번 뜬다', async () => {
    getItemAsync.mockResolvedValueOnce('saved-id');

    await getInstallationId();
    await getInstallationId();

    expect(getItemAsync).toHaveBeenCalledTimes(1);
  });

  it('★ 저장소가 실패해도 던지지 않고 null을 준다 — 등록이 막히면 푸시가 통째로 끊긴다', async () => {
    getItemAsync.mockRejectedValueOnce(new Error('keychain locked'));

    await expect(getInstallationId()).resolves.toBeNull();
  });
});
