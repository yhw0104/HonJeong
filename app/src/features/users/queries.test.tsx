// useMyProfile의 enabled 옵션 — 이게 없으면(또는 무시되면) 비로그인 상태에서도
// GET /users/me가 나가 401 → refresh 실패 → 세션 만료 캐스케이드가 돈다
// (app/src/shared/ws/useChatSocket.ts가 이 옵션으로 guest·loading을 막는다).
//
// 실제 useQuery/QueryClient를 그대로 써서, "enabled:false가 fetchMyProfile 자체를
// 안 부른다"를 진짜로 증명한다(옵션이 전달만 되고 무시되는 상황도 잡아낸다).
import React from 'react';
import TestRenderer, { act } from 'react-test-renderer';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useMyProfile } from './queries';
import { fetchMyProfile } from './api';

jest.mock('./api', () => ({
  fetchMyProfile: jest.fn(async () => ({ id: 1 })),
  updateMyProfile: jest.fn(),
  fetchActivitySummary: jest.fn(),
}));

function setup(options?: { enabled?: boolean }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  function Probe() {
    useMyProfile(options);
    return null;
  }
  act(() => {
    TestRenderer.create(
      <QueryClientProvider client={qc}>
        <Probe />
      </QueryClientProvider>,
    );
  });
}

describe('useMyProfile', () => {
  // react-query의 notifyManager가 배치 알림을 setTimeout(0)으로 스케줄한다 — 가짜 타이머로
  // 묶어 두지 않으면 목 fetchMyProfile()의 응답이 테스트 종료 뒤 act() 밖에서 상태를 갱신해
  // 경고가 뜨고 프로세스가 안 끝난다(client.test.ts의 fake timers 패턴과 동일한 이유).
  beforeEach(() => {
    jest.useFakeTimers();
    (fetchMyProfile as jest.Mock).mockClear();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('옵션 없이 부르면 기존 호출부와 동일하게 즉시 조회한다(하위 호환)', () => {
    setup();
    expect(fetchMyProfile).toHaveBeenCalledTimes(1);
  });

  it('★enabled:false면 GET /users/me를 아예 부르지 않는다 — guest 상태에서 이게 새면 세션 만료 캐스케이드가 돈다', () => {
    setup({ enabled: false });
    expect(fetchMyProfile).not.toHaveBeenCalled();
  });

  it('enabled:true는 옵션 없을 때와 같다', () => {
    setup({ enabled: true });
    expect(fetchMyProfile).toHaveBeenCalledTimes(1);
  });
});
