import { apiGet, apiPost, apiDelete } from '@/shared/api/client';
import type { ReportReason } from './reportCopy';

export type ReportTargetType = 'USER' | 'REVIEW';

export type BlockedUser = {
  userId: number;
  nickname: string | null;
  profileImageUrl: string | null;
  createdAt: string;
};

export type MyReport = {
  id: number;
  targetType: ReportTargetType;
  targetNickname: string;
  reasonCode: ReportReason | string;
  detail: string | null;
  status: string;
  createdAt: string;
};

export const blockUser = (targetUserId: number) => apiPost<null>('/blocks', { targetUserId });
export const unblockUser = (targetUserId: number) => apiDelete<null>(`/blocks/${targetUserId}`);
export const fetchBlockedUsers = () => apiGet<BlockedUser[]>('/blocks');

export const createReport = (body: {
  targetType: ReportTargetType;
  targetId: number;
  reasonCode: string;
  detail?: string;
}) => apiPost<{ reportId: number; status: string }>('/reports', body);
export const fetchMyReports = () => apiGet<MyReport[]>('/reports');
