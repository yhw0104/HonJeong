import { apiGet } from '@/shared/api/client';

// GET /notices — 공지 목록(핀 우선·게시 최신순, 서버가 미래 게시분 제외).
export type Notice = {
  id: number;
  category: 'UPDATE' | 'EVENT' | 'GENERAL' | string;
  title: string;
  body: string | null;
  pinned: boolean;
  publishedAt: string;
};

export const fetchNotices = () => apiGet<{ notices: Notice[] }>('/notices');
