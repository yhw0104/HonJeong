// 기기 토큰 등록·해제 HTTP. push/index.ts만 이 파일을 부른다(화면은 직접 부르지 않는다).
import { apiDelete, apiPost } from '@/shared/api/client';

/** 서버가 받는 플랫폼 값 — 백엔드 Platform enum과 문자열이 같아야 한다. */
export type PushPlatform = 'IOS' | 'ANDROID';

/**
 * 토큰 등록. 서버는 UPSERT다 — 같은 토큰이 이미 있으면 주인을 지금 사용자로 갱신한다.
 *
 * installationId를 함께 보내면 서버가 같은 기기의 옛 토큰을 정리한다(로그아웃 때 FCM 폐기가
 * 실패해 주인 없이 남은 행). 값이 없으면 보내지 않는다 — 서버는 선택 항목으로 받는다.
 */
export const registerDeviceToken = (token: string, platform: PushPlatform, installationId: string | null) =>
  apiPost<null>('/device-tokens', installationId ? { token, platform, installationId } : { token, platform });

// 토큰을 경로가 아니라 본문에 담는다 — 경로에 넣으면 접근 로그에 남는다.
// 이걸 위해 client.ts의 apiDelete에 body 파라미터를 추가했다.
export const deleteDeviceToken = (token: string) => apiDelete<null>('/device-tokens', { token });
