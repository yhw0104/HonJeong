import { useEffect } from 'react';
import { StatusBar } from 'expo-status-bar';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { NavigationContainer } from '@react-navigation/native';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from '@/shared/auth/AuthContext';
import { RootNavigator } from '@/navigation/RootNavigator';
import { setupRealtimeFocus } from '@/shared/realtime';
import { ErrorBoundary } from '@/shared/components';
import { initKakao } from '@/features/auth/kakaoLogin';

// 앱 전역 데이터 캐시("공용 게시판"). 여러 화면이 같은 캐시를 구독해 한 곳이 바뀌면 함께 갱신된다.
const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1, staleTime: 10_000 } },
});

// 앱 시작 시 1회만 실행되어야 하므로 컴포넌트 바깥(모듈 최상위)에서 호출한다.
initKakao();

export default function App() {
  // 앱이 포그라운드로 돌아오면 활성 쿼리를 즉시 갱신(폴링과 함께 실시간성 확보).
  useEffect(() => setupRealtimeFocus(), []);

  return (
    <SafeAreaProvider>
      {/* 앱 전역 렌더 에러 안전망 — 크래시 시 흰 화면 대신 복구 UI. providers 위를 감싼다. */}
      <ErrorBoundary>
        <QueryClientProvider client={queryClient}>
          <AuthProvider>
            <NavigationContainer>
              <StatusBar style="dark" />
              <RootNavigator />
            </NavigationContainer>
          </AuthProvider>
        </QueryClientProvider>
      </ErrorBoundary>
    </SafeAreaProvider>
  );
}
