// 훅 자체를 렌더해 검증한다(react-test-renderer로 zero-dependency 하네스 — 패턴은
// shared/useDebouncedValue.test.tsx 참고). createChatSocket과 useAuth·fetchMyProfile의
// 네트워크만 목으로 막고, useMyProfile·QueryClient는 실물을 그대로 쓴다 — enabled 게이트가
// 실제로 fetchMyProfile 호출을 막는지까지 증명하기 위해서다(목만 잔뜩 쌓아 "불렸다"만
// 확인하는 테스트는 정직하지 않다는 지적을 받은 뒤 이렇게 바꿨다).
import React from 'react';
import TestRenderer, { act } from 'react-test-renderer';
import { AppState } from 'react-native';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import { useChatSocket } from './useChatSocket';
import { conversationKeys } from '@/features/chat/queries';
import { fetchMyProfile } from '@/features/users/api';
import type { MyProfile } from '@/features/users/api';
import type { ConversationSummary } from '@/features/chat/types';
import type { WsMessageEvent, WsEvent } from './types';

// --- useAuth: status를 테스트에서 직접 조작한다(모듈 스코프 가변 객체) ---
// jest.mock 팩토리는 호이스팅되어 "mock"으로 시작하는 이름만 바깥 변수 참조가 허용된다
// (babel-plugin-jest-hoist 규칙) — 그래서 mock으로 시작하는 이름을 쓴다.
const mockAuthState: { status: 'loading' | 'authed' | 'guest' } = { status: 'authed' };
jest.mock('@/shared/auth/AuthContext', () => ({
  useAuth: () => mockAuthState,
}));

// --- fetchMyProfile: 진짜 useMyProfile/useQuery가 이걸 부른다. guest 게이트가 실제로
//     이 호출 자체를 막는지 확인하는 것이 Finding 1의 핵심 증거다. ---
jest.mock('@/features/users/api', () => ({
  fetchMyProfile: jest.fn(),
  updateMyProfile: jest.fn(),
  fetchActivitySummary: jest.fn(),
}));

// --- createChatSocket: connect/disconnect 호출과 onEvent 콜백을 잡아 둔다(client.test.ts에서
//     이미 소켓 자체는 충분히 검증했으므로 여기서는 재검증하지 않는다) ---
const mockConnect = jest.fn();
const mockDisconnect = jest.fn();
const mockSocketHandlers: { onEvent: ((e: WsEvent) => void) | null } = { onEvent: null };
jest.mock('./client', () => ({
  createChatSocket: ({ onEvent }: { onEvent: (e: WsEvent) => void }) => {
    mockSocketHandlers.onEvent = onEvent;
    return { connect: mockConnect, disconnect: mockDisconnect };
  },
}));

const removeMock = jest.fn();
let changeHandler: ((s: string) => void) | null = null;

// react-query의 notifyManager는 배치 알림을 setTimeout(0)으로 스케줄한다. 가짜 타이머로
// 묶고 flush()에서 같이 흘려보내지 않으면, 목 fetchMyProfile()이 응답한 뒤의 리렌더가
// act() 밖에서 일어나 경고가 뜨고(react-query 실물을 쓰는 이 파일 특유의 문제 —
// createChatSocket만 목인 client.test.ts에는 없던 원인) 타이머가 프로세스를 안 끝낸다.
//
// ★runOnlyPendingTimers가 아니라 advanceTimersByTime(0)을 쓴다 — setQueryData로 만든
//   쿼리는 관찰자가 없어 마운트 즉시 gcTime(5분) 타이머가 걸리는데, runOnlyPendingTimers는
//   그 타이머까지 "지금 걸려 있다"는 이유로 즉시 당겨 실행해 캐시를 지워버린다. 0ms만
//   당기면 정말 0ms짜리(notifyManager)만 돈다.
async function flush() {
  await Promise.resolve();
  await Promise.resolve();
  act(() => { jest.advanceTimersByTime(0); });
}

const PROFILE_1: MyProfile = {
  id: 1,
  nickname: null,
  profileImageUrl: null,
  region: null,
  regionLat: null,
  regionLng: null,
  introduction: null,
  diningStyle: null,
  gender: null,
  ageGroup: null,
  favoriteFoods: [],
};

const CONV: ConversationSummary = {
  conversationId: 1,
  status: 'ACTIVE',
  partnerUserId: 2,
  partnerNickname: '상대',
  partnerProfileImageUrl: null,
  placeName: '식당',
  lastMessagePreview: null,
  lastMessageAt: null,
  unreadCount: 0,
  partnerLastReadAt: null,
  createdAt: '2026-01-01T00:00:00Z',
  muted: false,
};

const MESSAGE_FROM_PARTNER: WsMessageEvent = {
  type: 'message',
  conversationId: 1,
  message: { id: 10, senderUserId: 2, type: 'TEXT', text: '안녕', imageUrl: null, createdAt: '2026-01-01T00:01:00Z' },
};

function setup(qc: QueryClient) {
  function Probe() {
    useChatSocket();
    return null;
  }
  // 매번 새 JSX를 만든다(useDebouncedValue.test.tsx와 같은 패턴) — 트리 객체를 재사용하면
  // 리렌더 시 참조가 같아 React가 재실행을 건너뛸 수 있다.
  const tree = () => (
    <QueryClientProvider client={qc}>
      <Probe />
    </QueryClientProvider>
  );
  let r!: TestRenderer.ReactTestRenderer;
  act(() => {
    r = TestRenderer.create(tree());
  });
  return {
    rerender: () => act(() => { r.update(tree()); }),
    unmount: () => act(() => { r.unmount(); }),
  };
}

function newClient() {
  return new QueryClient({ defaultOptions: { queries: { retry: false } } });
}

beforeEach(() => {
  jest.useFakeTimers();
  mockAuthState.status = 'authed';
  mockConnect.mockClear();
  mockDisconnect.mockClear();
  removeMock.mockClear();
  mockSocketHandlers.onEvent = null;
  changeHandler = null;
  (fetchMyProfile as jest.Mock).mockReset().mockResolvedValue(PROFILE_1);
  (AppState.addEventListener as jest.Mock).mockReset().mockImplementation((type: string, handler: (s: string) => void) => {
    if (type === 'change') changeHandler = handler;
    return { remove: removeMock };
  });
  (AppState as unknown as { currentState: string }).currentState = 'active';
});

afterEach(() => {
  jest.useRealTimers();
});

describe('useChatSocket', () => {
  it('★guest 상태에서는 내 프로필을 조회하지 않는다 — 새면 GET /users/me가 401을 내고 세션 만료 캐스케이드가 돈다', async () => {
    mockAuthState.status = 'guest';
    setup(newClient());
    await act(async () => { await flush(); });

    expect(fetchMyProfile).not.toHaveBeenCalled();
    // guest이므로 소켓도 만들어지지 않는다(기존 가드 — 이번 수정과 무관하게 유지돼야 한다).
    expect(mockConnect).not.toHaveBeenCalled();
  });

  it('내 uid가 아직 없으면(프로필 로딩 중) 이벤트를 캐시에 반영하지 않고, 프로필이 오면 이후 이벤트부터 반영한다', async () => {
    let resolveProfile!: (v: MyProfile) => void;
    (fetchMyProfile as jest.Mock).mockReset().mockImplementation(
      () => new Promise<MyProfile>((res) => { resolveProfile = res; }),
    );

    const qc = newClient();
    qc.setQueryData(conversationKeys.list, [CONV]);
    setup(qc);

    // 프로필 응답 전 — uid가 없으므로 이벤트가 버려진다.
    act(() => { mockSocketHandlers.onEvent?.(MESSAGE_FROM_PARTNER); });
    expect(qc.getQueryData<ConversationSummary[]>(conversationKeys.list)?.[0].unreadCount).toBe(0);

    await act(async () => {
      resolveProfile(PROFILE_1);
      await flush();
    });

    // 이제 uid=1이 확보됐다 — 같은 이벤트가 이번엔 반영된다(상대가 보냈으므로 안읽음 +1).
    act(() => { mockSocketHandlers.onEvent?.(MESSAGE_FROM_PARTNER); });
    expect(qc.getQueryData<ConversationSummary[]>(conversationKeys.list)?.[0].unreadCount).toBe(1);
  });

  it('포그라운드로 오면 connect + [chat] 무효화, 백그라운드로 가면 disconnect', async () => {
    const qc = newClient();
    const invalidateSpy = jest.spyOn(qc, 'invalidateQueries');
    setup(qc); // 마운트 시 currentState='active' → 최초 sync(true)

    expect(mockConnect).toHaveBeenCalledTimes(1);
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['chat'] });

    invalidateSpy.mockClear();
    act(() => { changeHandler?.('background'); });
    expect(mockDisconnect).toHaveBeenCalledTimes(1);
    expect(mockConnect).toHaveBeenCalledTimes(1); // 추가 connect 없음

    act(() => { changeHandler?.('active'); });
    expect(mockConnect).toHaveBeenCalledTimes(2);
    // ★재연결 시 갱 복구 — 끊긴 사이의 메시지를 폴링(30초) 전에 따라잡는다.
    expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: ['chat'] });
  });

  it('로그아웃(status: authed→guest)하면 소켓을 끊고 AppState 리스너도 정리한다', async () => {
    const qc = newClient();
    const { rerender } = setup(qc);
    expect(mockConnect).toHaveBeenCalledTimes(1);

    mockAuthState.status = 'guest';
    rerender();

    expect(mockDisconnect).toHaveBeenCalledTimes(1); // 이전 이펙트의 cleanup에서 끊는다
    expect(removeMock).toHaveBeenCalledTimes(1); // AppState 구독도 정리
    expect(mockConnect).toHaveBeenCalledTimes(1); // guest 이펙트는 즉시 리턴 — 재연결 없음
  });
});
