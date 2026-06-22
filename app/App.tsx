import { StatusBar } from 'expo-status-bar';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { NavigationContainer } from '@react-navigation/native';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from '@/shared/auth/AuthContext';
import { RootNavigator } from '@/navigation/RootNavigator';

// 앱 전역 데이터 캐시("공용 게시판"). 여러 화면이 같은 캐시를 구독해 한 곳이 바뀌면 함께 갱신된다.
const queryClient = new QueryClient({
  defaultOptions: { queries: { retry: 1, staleTime: 10_000 } },
});

export default function App() {
  return (
    <SafeAreaProvider>
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <NavigationContainer>
            <StatusBar style="dark" />
            <RootNavigator />
          </NavigationContainer>
        </AuthProvider>
      </QueryClientProvider>
    </SafeAreaProvider>
  );
}
