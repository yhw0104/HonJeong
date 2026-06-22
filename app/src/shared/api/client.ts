// 백엔드 호출용 얇은 fetch 래퍼.
// 베이스 URL + '/api' 접두사와 JSON 헤더를 붙이고, 공통 응답 엔벨로프({success,data,error})를
// 풀어 성공이면 data만 돌려주고 실패면 ApiError를 던진다. (인증 토큰 주입은 인증 슬라이스에서 추가 예정.)
import { API_BASE_URL } from '@/shared/config/api';
import { getAccessToken } from '@/shared/auth/session';

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

type Method = 'GET' | 'POST' | 'PATCH';

/**
 * 호출 옵션. token을 명시하면 그 값으로 Authorization을 정한다:
 *   - 문자열  → 그 토큰을 Bearer로 첨부(예: 온보딩 토큰으로 terms/complete 호출)
 *   - null    → 인증 헤더 없이 호출(예: 토큰 재발급/공개 엔드포인트)
 *   - 미지정  → 세션에 저장된 access 토큰을 자동 첨부(기본 동작)
 */
type RequestOptions = { token?: string | null };

/**
 * 백엔드 API를 한 번 호출한다. `${API_BASE_URL}/api${path}`로 요청하고 공통 엔벨로프를 푼다.
 * 성공이면 data(T)를 반환하고, 네트워크 실패·비2xx·success:false면 {@link ApiError}를 던진다.
 */
async function request<T>(method: Method, path: string, body?: unknown, options?: RequestOptions): Promise<T> {
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
    const code = envelope?.error?.code ?? `HTTP_${res.status}`;
    const message = envelope?.error?.message ?? `요청 실패 (HTTP ${res.status})`;
    throw new ApiError(res.status, code, message);
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
