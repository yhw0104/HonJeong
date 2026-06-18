// 인증 세션 — access/refresh 토큰을 메모리 + SecureStore에 보관한다.
//   - 메모리: 매 API 요청에서 동기적으로 빠르게 읽기(클라이언트의 Bearer 첨부).
//   - SecureStore: 앱을 껐다 켜도 토큰이 남아 자동로그인이 되도록 영속 저장(OS 보안 저장소).
import * as SecureStore from 'expo-secure-store';

const ACCESS_KEY = 'honjeong.accessToken';
const REFRESH_KEY = 'honjeong.refreshToken';

/** 로그인/재발급 응답에서 받는 토큰 쌍. (서버 응답의 expiresIn은 현재 사용하지 않아 받지 않는다.) */
export type Tokens = { accessToken: string; refreshToken: string };

let accessToken: string | null = null;
let refreshToken: string | null = null;

/** 현재 access 토큰(메모리). API 클라이언트가 Authorization 헤더 첨부에 사용한다. */
export function getAccessToken(): string | null {
  return accessToken;
}

/** 현재 refresh 토큰(메모리). 자동로그인(재발급)·로그아웃(무효화)에 사용한다. */
export function getRefreshToken(): string | null {
  return refreshToken;
}

/** 토큰을 메모리와 SecureStore에 저장한다(로그인 성공·재발급 시). */
export async function setTokens(tokens: Tokens): Promise<void> {
  accessToken = tokens.accessToken;
  refreshToken = tokens.refreshToken;
  await SecureStore.setItemAsync(ACCESS_KEY, tokens.accessToken);
  await SecureStore.setItemAsync(REFRESH_KEY, tokens.refreshToken);
}

/** 토큰을 메모리와 SecureStore에서 모두 제거한다(로그아웃·세션 만료). */
export async function clearTokens(): Promise<void> {
  accessToken = null;
  refreshToken = null;
  await SecureStore.deleteItemAsync(ACCESS_KEY);
  await SecureStore.deleteItemAsync(REFRESH_KEY);
}

/**
 * SecureStore에 저장된 토큰을 메모리로 읽어온다(앱 시작 시 1회).
 * @returns 저장돼 있던 refresh 토큰(없으면 null) — 자동로그인 시도 여부 판단에 쓴다.
 */
export async function loadTokens(): Promise<string | null> {
  accessToken = await SecureStore.getItemAsync(ACCESS_KEY);
  refreshToken = await SecureStore.getItemAsync(REFRESH_KEY);
  return refreshToken;
}
