// 서버가 소켓으로 내려보내는 이벤트. 서버의 WsMessageEvent·WsReadEvent와 짝이다.
import type { ChatMessage } from '@/features/chat/types';

export type WsMessageEvent = { type: 'message'; conversationId: number; message: ChatMessage };
export type WsReadEvent = { type: 'read'; conversationId: number; readerUserId: number; readAt: string };
export type WsPongEvent = { type: 'pong' };

export type WsEvent = WsMessageEvent | WsReadEvent | WsPongEvent;
