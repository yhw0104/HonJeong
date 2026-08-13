// API 베이스 주소에서 소켓 주소를 만든다.
// 주소를 한 곳에서만 만들어야 https/http 짝이 어긋나지 않는다(운영은 wss, 로컬은 ws).

/**
 * @param apiBaseUrl `API_BASE_URL`(예: https://honjeong-api.com)
 * @param ticket 서버에서 받은 1회용 티켓
 * @returns 소켓 주소
 */
export function toWsUrl(apiBaseUrl: string, ticket: string): string {
  const base = apiBaseUrl.replace(/\/+$/, '').replace(/^http/, 'ws');
  return `${base}/ws?ticket=${encodeURIComponent(ticket)}`;
}
