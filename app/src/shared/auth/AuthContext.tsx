// 인증 상태 컨텍스트 — 앱 전역의 로그인 여부와 로그인/로그아웃 동작을 제공한다.
//
// status:
//   - 'loading' : 앱 시작 직후, 저장된 토큰으로 세션 복원을 시도하는 중(스플래시)
//   - 'authed'  : 로그인됨 → 메인 화면
//   - 'guest'   : 미로그인 → 온보딩 화면
//
// 자동로그인은 "앱 시작 시 저장된 refresh 토큰으로 1회 재발급"으로 구현한다(refresh 수명 14일).
// access 토큰은 1시간이라 그것만으론 부족하므로, 시작 시 refresh로 새 토큰 쌍을 받아 세션을 복원한다.
// (요청 도중 401이 나면 자동 재시도하는 per-request refresh는 다음 단계로 분리.)
import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { ActivityIndicator, View } from 'react-native';
import { useQueryClient } from '@tanstack/react-query';

import { apiPost, setOnSessionExpired } from '@/shared/api/client';
import { clearTokens, getRefreshToken, loadTokens, setTokens, type Tokens } from '@/shared/auth/session';
import { registerPushToken, revokePushToken, unregisterPushToken } from '@/shared/push';

type AuthStatus = 'loading' | 'authed' | 'guest';

type AuthContextValue = {
  /** 현재 인증 상태. RootNavigator가 이 값으로 보여줄 화면 그룹을 고른다. */
  status: AuthStatus;
  /** 로그인 확정 — 토큰을 저장하고 상태를 authed로 전환한다(온보딩 완료/기존 회원 로그인). */
  signIn: (tokens: Tokens) => Promise<void>;
  /** 로그아웃 — 서버 refresh 무효화 시도 후 로컬 토큰을 비우고 guest로 전환한다. */
  signOut: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>('loading');
  const queryClient = useQueryClient(); // App.tsx에서 QueryClientProvider 안쪽에 배치됨

  // 앱 시작 시 1회: 저장된 refresh 토큰으로 세션 복원을 시도한다.
  useEffect(() => {
    let alive = true;
    (async () => {
      const refresh = await loadTokens();
      if (!refresh) {
        if (alive) setStatus('guest'); // 저장된 세션 없음 → 온보딩
        return;
      }
      try {
        // refresh는 공개 엔드포인트라 토큰 없이 호출(token: null). 성공하면 새 토큰 쌍으로 교체.
        const tokens = await apiPost<Tokens>('/auth/refresh', { refreshToken: refresh }, { token: null });
        await setTokens(tokens);
        // 앱을 켤 때마다 푸시 토큰을 재등록한다 — FCM이 토큰을 바꿔도 서버가 따라간다.
        // 결과를 기다리지 않는다(세션 복원을 늦추지 않게). 권한이 없으면 내부에서 아무것도 하지 않는다.
        void registerPushToken();
        if (alive) setStatus('authed');
      } catch {
        await clearTokens(); // 만료/무효화된 refresh → 세션 폐기하고 온보딩으로
        if (alive) setStatus('guest');
      }
    })();
    return () => {
      alive = false;
    };
  }, []);

  // 요청 중 refresh까지 실패(세션 만료) 시 조용히 로그아웃. client가 React 밖이라 콜백으로 연결.
  useEffect(() => {
    setOnSessionExpired(() => {
      // 기기의 FCM 토큰을 폐기한다. 여기선 access가 이미 무효라 DELETE /device-tokens를 못 부르고,
      // 서버 행은 그대로 남아 이전 사용자의 알림이 이 폰에 계속 배달된다("{닉네임}: {메시지}"가
      // 잠금화면에 뜬다). 아무도 이 기기에서 다시 로그인하지 않으면 등록 UPSERT도 영영 안 돌므로,
      // 서버가 아니라 기기 쪽에서 끊는다. 다음 로그인 때 새 토큰이 발급돼 재등록된다.
      void revokePushToken();
      void clearTokens();
      queryClient.clear();
      setStatus('guest');
    });
    return () => setOnSessionExpired(null);
  }, [queryClient]);

  const signIn = useCallback(async (tokens: Tokens) => {
    await setTokens(tokens);
    void registerPushToken(); // 결과를 기다리지 않는다 — 로그인 전환을 늦추지 않게
    setStatus('authed');
  }, []);

  const signOut = useCallback(async () => {
    // 푸시 정리는 두 단계다. 서버 삭제는 /auth/logout '앞'에서 한다 — 이 시점에는 access가 아직 유효하다.
    // 이어서 기기 토큰까지 폐기한다: 서버 DELETE가 네트워크 실패로 못 지나가면 행이 남아
    // 로그아웃한 폰에 이전 사용자의 알림이 계속 뜨기 때문이다(세션 만료 경로와 같은 구멍).
    // 순서가 중요하다 — 서버 삭제가 토큰 값을 필요로 하므로 폐기는 그 뒤에 온다.
    await unregisterPushToken();
    await revokePushToken();
    // 서버에 refresh 무효화를 알린다(실패해도 로컬은 반드시 정리). access 토큰은 클라이언트가 자동 첨부.
    const refresh = getRefreshToken();
    if (refresh) {
      try {
        await apiPost('/auth/logout', { refreshToken: refresh });
      } catch {
        /* 네트워크 실패 등은 무시하고 로컬 정리로 진행 */
      }
    }
    await clearTokens();
    // 이전 계정의 캐시(프로필·체크인 등)가 재로그인 시 잠깐 노출되지 않도록 전부 비운다.
    queryClient.clear();
    setStatus('guest');
  }, [queryClient]);

  // 세션 복원 중에는 스플래시(빈 로딩 화면)를 보여준다.
  if (status === 'loading') {
    return (
      <View style={{ flex: 1, alignItems: 'center', justifyContent: 'center' }}>
        <ActivityIndicator />
      </View>
    );
  }

  return <AuthContext.Provider value={{ status, signIn, signOut }}>{children}</AuthContext.Provider>;
}

/** 인증 컨텍스트 접근 훅. AuthProvider 하위에서만 사용. */
export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
