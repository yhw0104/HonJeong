import { apiPost } from '@/shared/api/client';

export type WsTicket = { ticket: string; expiresInSeconds: number };

/**
 * 소켓 연결용 1회용 티켓을 받는다.
 *
 * 일반 REST라 401 자동 refresh·탈퇴/정지 차단이 그대로 적용된다 —
 * 그래서 핸드셰이크에서 그 판정을 다시 하지 않는다.
 */
export const fetchWsTicket = () => apiPost<WsTicket>('/ws-ticket');
