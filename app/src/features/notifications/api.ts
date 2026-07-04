import { apiGet, apiPatch } from '@/shared/api/client';

export type NotificationType =
  | 'MEAL_REQUEST_RECEIVED'
  | 'MEAL_REQUEST_ACCEPTED'
  | 'MATE_REQUEST_RECEIVED'
  | 'MATE_REQUEST_ACCEPTED';

export type NotificationItem = {
  id: number;
  type: NotificationType;
  actorNickname: string | null;
  isRead: boolean;
  createdAt: string;
};

export const fetchNotifications = () => apiGet<NotificationItem[]>('/notifications');
export const fetchUnreadCount = () => apiGet<{ count: number }>('/notifications/unread-count');
export const markNotificationRead = (id: number) => apiPatch<null>(`/notifications/${id}/read`);
export const markAllNotificationsRead = () => apiPatch<null>('/notifications/read-all');
