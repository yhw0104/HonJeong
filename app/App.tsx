import { useEffect } from 'react';
import { StatusBar } from 'expo-status-bar';
import { GestureHandlerRootView } from 'react-native-gesture-handler';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { NavigationContainer } from '@react-navigation/native';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from '@/shared/auth/AuthContext';
import { RootNavigator } from '@/navigation/RootNavigator';
import { navigationRef } from '@/navigation/navigationRef';
import { setupRealtimeFocus } from '@/shared/realtime';
import { usePushMessaging } from '@/shared/push/usePushMessaging';
import { PushBanner } from '@/shared/push/PushBanner';
import { ErrorBoundary } from '@/shared/components';
import { initKakao } from '@/features/auth/kakaoLogin';

// 앱 전역 데이터 캐시("공용 게시판"). 여러 화면이 같은 캐시를 구독해 한 곳이 바뀌면 함께 갱신된다.
const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1, staleTime: 10_000 } },
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
