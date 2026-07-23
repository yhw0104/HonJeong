import { apiGet, apiPatch } from '@/shared/api/client';

export type NotificationType =
  | 'MEAL_REQUEST_RECEIVED'
  | 'MEAL_REQUEST_ACCEPTED'
  | 'MEAL_MATCH_CANCELLED'
  | 'MATE_REQUEST_RECEIVED'
  | 'MATE_REQUEST_ACCEPTED'
  | 'BADGE_EARNED';

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

export type NotificationSettings = {
  meal: boolean;
  mate: boolean;
  notice: boolean;
  marketing: boolean;
};

export const fetchNotificationSettings = () =>
  apiGet<NotificationSettings>('/notifications/settings');

export const updateNotificationSettings = (settings: NotificationSettings) =>
  apiPatch<NotificationSettings>('/notifications/settings', settings);
