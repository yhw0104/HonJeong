// 백엔드 호출용 얇은 fetch 래퍼.
// 베이스 URL + '/api' 접두사와 JSON 헤더를 붙이고, 공통 응답 엔벨로프({success,data,error})를
// 풀어 성공이면 data만 돌려주고 실패면 ApiError를 던진다. (인증 토큰 주입은 인증 슬라이스에서 추가 예정.)
import { API_BASE_URL } from '@/shared/config/api';
import { getAccessToken, getRefreshToken, setTokens, type Tokens } from '@/shared/auth/session';

/** 백엔드 공통 응답 엔벨로프: 성공 {success:true,data}, 실패 {success:false,error:{code,message}}. */
type ApiEnvelope<T> = {
  success: boolean;
  data?: T;
  error?: { code: string; message: string };
};

/** API 호출 실패. HTTP 상태와 서버 에러코드/메시지를 담는다(네트워크 실패는 status=0). */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

type Method = 'GET' | 'POST' | 'PATCH' | 'DELETE';

/**
 * 호출 옵션. token을 명시하면 그 값으로 Authorization을 정한다:
 *   - 문자열  → 그 토큰을 Bearer로 첨부(예: 온보딩 토큰으로 terms/complete 호출)
 *   - null    → 인증 헤더 없이 호출(예: 토큰 재발급/공개 엔드포인트)
 *   - 미지정  → 세션에 저장된 access 토큰을 자동 첨부(기본 동작)
 */
type RequestOptions = { token?: string | null };

// 세션 요청 여부: options에 token 키가 없으면 세션 access를 자동 첨부하는 요청.
function isSessionAuthed(options?: RequestOptions): boolean {
  return !(options && 'token' in options);
}

/** 401이고 세션 요청이며 아직 재시도 전이면 refresh 시도 대상. (순수) */
export function shouldAttemptRefresh(status: number, sessionAuthed: boolean, retried: boolean): boolean {
  return status === 401 && sessionAuthed && !retried;
}

/**
 * refresh 실패를 '세션 만료'로 볼 것인가 — 서버가 401로 거부했을 때만 참이다. (순수)
 *
 * ★ 여기서 걸러내지 않으면 서버가 잠깐 죽은 것이 강제 로그아웃이 된다. 실제로 겪는 경로는
 * 배포다: `docker compose up -d --build app`으로 컨테이너가 재시작되는 동안, access가 만료된
 * 요청이 401을 받고 이어지는 /auth/refresh가 업스트림 없는 Caddy에 닿아 502를 받는다.
 * 그 502를 만료로 취급하면 세션이 날아가는데, refresh 토큰은 멀쩡히 살아 있었다.
 * 푸시가 붙은 뒤로는 대가가 더 커졌다 — onSessionExpired가 기기 FCM 토큰까지 폐기한다.
 *
 * 401만 만료로 보는 근거: 서버는 리프레시 토큰이 무효일 때 INVALID_REFRESH_TOKEN(401)을 준다
 * (backend ErrorCode.java). 네트워크 실패(status 0)·5xx는 "모른다"이지 "무효다"가 아니므로
 * 세션을 유지하고, 다음 요청이 다시 시도한다.
 *
 * @param error refreshSession()이 던진 값
 * @returns 서버가 리프레시 토큰을 거부한 것이면 true
 */
export function isAuthRejection(error: unknown): boolean {
  return error instanceof ApiError && error.status === 401;
}

let refreshInFlight: Promise<void> | null = null;
let onSessionExpired: (() => void) | null = null;

/** 세션 만료(refresh 실패) 시 호출될 콜백 등록. AuthContext가 마운트 시 설정. */
export function setOnSessionExpired(cb: (() => void) | null): void {
  onSessionExpired = cb;
}

/** 등록된 세션 만료 콜백을 호출한다(request() 밖의 경로 — 예: 파일 업로드 — 에서 만료 처리에 사용). */
export function notifySessionExpired(): void {
  onSessionExpired?.();
}

/** refresh 토큰으로 새 토큰 쌍 발급. single-flight — 동시 호출은 하나의 refresh를 공유(회전 토큰 stale 방지).
 *  request() 401 재시도와 request() 밖 경로(파일 업로드)가 공유한다. */
export function refreshSession(): Promise<void> {
  if (!refreshInFlight) {
    refreshInFlight = (async () => {
      const rt = getRefreshToken();
      if (!rt) throw new ApiError(401, 'INVALID_REFRESH_TOKEN', '세션이 만료되었습니다.');
      // token:null → 이 요청은 refresh 로직 대상에서 제외(재귀 없음).
      const tokens = await request<Tokens>('POST', '/auth/refresh', { refreshToken: rt }, { token: null });
      await setTokens(tokens);
    })().finally(() => {
      refreshInFlight = null;
    });
  }
  return refreshInFlight;
}

/**
 * 백엔드 API를 한 번 호출한다. `${API_BASE_URL}/api${path}`로 요청하고 공통 엔벨로프를 푼다.
 * 성공이면 data(T)를 반환하고, 네트워크 실패·비2xx·success:false면 {@link ApiError}를 던진다.
 */
async function request<T>(
  method: Method,
  path: string,
  body?: unknown,
  options?: RequestOptions,
  retried = false,
): Promise<T> {
  // options.token이 명시(문자열/null)되면 그 값을, 아니면 세션의 access 토큰을 사용한다.
  const token = options && 'token' in options ? options.token : getAccessToken();
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = `Bearer ${token}`;

  const res = await fetch(`${API_BASE_URL}/api${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  }).catch(() => {
    // 서버 꺼짐·URL 오류 등 연결 자체 실패 → status 0으로 통일.
    throw new ApiError(0, 'NETWORK_ERROR', '서버에 연결할 수 없습니다.');
  });

  let envelope: ApiEnvelope<T> | null = null;
  try {
    envelope = (await res.json()) as ApiEnvelope<T>;
  } catch {
    // 본문이 JSON이 아닐 수 있음(일부 오류 응답) → 아래에서 HTTP 상태로 처리.
    envelope = null;
  }

  if (!res.ok || !envelope || !envelope.success) {
    const err = new ApiError(
      res.status,
      envelope?.error?.code ?? `HTTP_${res.status}`,
      envelope?.error?.message ?? `요청 실패 (HTTP ${res.status})`,
    );
    // 세션 요청이 401이면 refresh 후 원요청을 1회 재시도. refresh가 '거부'당했으면 세션 만료 처리.
    if (shouldAttemptRefresh(res.status, isSessionAuthed(options), retried)) {
      try {
        await refreshSession();
      } catch (refreshError) {
        if (isAuthRejection(refreshError)) onSessionExpired?.();
        throw err;
      }
      return request<T>(method, path, body, options, true);
    }
    throw err;
  }
  // data가 생략된 성공 응답(백엔드 @JsonInclude(NON_NULL)으로 null이면 키째 빠짐)은 null로 통일한다.
  // React Query는 쿼리 함수가 undefined를 반환하면 에러를 내므로, undefined가 새 나가지 않게 막는다.
  return (envelope.data ?? null) as T;
}

/** GET 요청(쿼리스트링은 path에 직접 포함). */
export const apiGet = <T>(path: string, options?: RequestOptions) => request<T>('GET', path, undefined, options);
/** POST 요청(JSON 본문). */
export const apiPost = <T>(path: string, body?: unknown, options?: RequestOptions) =>
  request<T>('POST', path, body, options);
/** PATCH 요청(JSON 본문). */
export const apiPatch = <T>(path: string, body?: unknown, options?: RequestOptions) =>
  request<T>('PATCH', path, body, options);
/** DELETE 요청(대부분 본문 없음). 응답은 200+엔벨로프(서버가 204 대신 {success:true} 반환).
 *  body를 받는 이유: 기기 토큰 해제(DELETE /device-tokens)는 토큰을 경로가 아니라 본문에 담는다
 *  — 경로에 넣으면 접근 로그에 그대로 남는다. 나머지 호출처는 인자가 path 하나뿐이라 영향이 없다. */
export const apiDelete = <T>(path: string, body?: unknown, options?: RequestOptions) =>
  request<T>('DELETE', path, body, options);
