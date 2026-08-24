import { useEffect } from 'react';
import { StatusBar } from 'expo-status-bar';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { NavigationContainer } from '@react-navigation/native';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ApiError } from '@/shared/api/client';
import { AuthProvider } from '@/shared/auth/AuthContext';
import { RootNavigator } from '@/navigation/RootNavigator';
import { navigationRef } from '@/navigation/navigationRef';
import { setupRealtimeFocus } from '@/shared/realtime';
import { usePushMessaging } from '@/shared/push/usePushMessaging';
import { PushBanner } from '@/shared/push/PushBanner';
import { useChatSocket } from '@/shared/ws/useChatSocket';
import { ErrorBoundary } from '@/shared/components';
import { initKakao } from '@/features/auth/kakaoLogin';

/**
 * 서버가 과부하일 때는 재시도하지 않는다.
 *
 * ★원래는 `retry: 1`이었다. 2026-08-24 부하 테스트에서 그 한 줄이 서버의 자가 회복을 막는 것이
 * 확인됐다 — 같은 서버에 같은 부하를 준 뒤 걷어냈을 때, 재시도가 없으면 60초 뒤 평상시로 완전히
 * 돌아왔지만(1.07배) 재시도가 있으면 13.16배 느린 상태에 그대로 머물렀다. 추가 트래픽은 15%
 * (149건/997건)뿐이었는데도 그렇다. 느려짐 → 실패 → 재시도 → 더 느려짐의 양성 피드백이 서버를
 * 그 상태에 고정시킨다(메타안정 장애).
 *
 * ★과부하 중에는 오히려 실패율이 낮게 보인다(30.3% → 12.8%). 재시도가 증상은 가리고 병은 키우므로
 * 에러율만 보는 모니터링으로는 이 장애를 잡을 수 없다.
 *
 * 그래서 재시도할 가치가 있는 경우에만 한 번 재시도한다:
 *   - 네트워크 오류(NETWORK_ERROR) — 지하철에서 잠깐 끊기는 등 즉시 복구되는 경우가 많다
 *   - 5xx·429는 재시도하지 않는다 — 서버가 힘들다는 신호이므로 물러나는 게 맞다
 *   - 4xx도 재시도하지 않는다 — 다시 보내도 결과가 같다
 */
function retryPolicy(failureCount: number, error: unknown): boolean {
  if (failureCount >= 1) return false;
  return error instanceof ApiError && error.code === 'NETWORK_ERROR';
}

// 앱 전역 데이터 캐시("공용 게시판"). 여러 화면이 같은 캐시를 구독해 한 곳이 바뀌면 함께 갱신된다.
const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: retryPolicy, staleTime: 10_000 } },
});

// 앱 시작 시 1회만 실행되어야 하므로 컴포넌트 바깥(모듈 최상위)에서 호출한다.
initKakao();

// 푸시 리스너는 useQueryClient를 쓰므로 QueryClientProvider 안쪽에서 불려야 한다.
// App 본체에서 부르면 provider 바깥이라 캐시에 닿지 못하므로 이 작은 컴포넌트를 끼운다.
//
// 인앱 배너도 여기서 그린다 — 배너 탭이 OS 배너 탭과 같은 이동 경로를 타야 해서
// 리스너가 내보낸 핸들러를 그대로 넘긴다(규칙이 두 벌이 되면 반드시 갈린다).
function PushBridge() {
  const openFromPush = usePushMessaging();
  // 채팅 소켓도 여기서 붙인다 — useQueryClient·useAuth를 쓰므로 두 provider 안쪽이어야 한다.
  useChatSocket();
  return <PushBanner onOpen={openFromPush} />;
}

export default function App() {
  // 앱이 포그라운드로 돌아오면 활성 쿼리를 즉시 갱신(폴링과 함께 실시간성 확보).
  useEffect(() => setupRealtimeFocus(), []);

  return (
    // 제스처의 진입점 — 문서 지침대로 루트에 최대한 가깝게 둔다(대화 목록 스와이프 삭제가 이 아래에서 동작).
    <GestureHandlerRootView style={{ flex: 1 }}>
      <SafeAreaProvider>
        {/* 앱 전역 렌더 에러 안전망 — 크래시 시 흰 화면 대신 복구 UI. providers 위를 감싼다. */}
        <ErrorBoundary>
          <QueryClientProvider client={queryClient}>
            <AuthProvider>
              {/* ref는 컴포넌트 밖(푸시 리스너)에서 화면을 이동하기 위한 것이다. */}
              <NavigationContainer ref={navigationRef}>
                <StatusBar style="dark" />
                <RootNavigator />
                {/* ★RootNavigator '뒤'에 둔다 — 배너가 화면 위에 그려져야 한다(형제 순서가 곧 층 순서). */}
                <PushBridge />
              </NavigationContainer>
            </AuthProvider>
          </QueryClientProvider>
        </ErrorBoundary>
      </SafeAreaProvider>
    </GestureHandlerRootView>
  );
}
