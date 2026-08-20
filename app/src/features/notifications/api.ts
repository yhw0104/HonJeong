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
  /**
   * 뱃지 획득 알림. 서버가 나중에 추가한 필드라 구버전 서버는 이 값을 안 준다 —
   * 그래도 화면(NotificationSettings.tsx)이 토글을 그리려면 값이 필요하므로,
   * undefined면 켜짐으로 본다(서버의 기본값과 같다).
   */
  badge?: boolean;
};

export const fetchNotificationSettings = () =>
  apiGet<NotificationSettings>('/notifications/settings');

export const updateNotificationSettings = (settings: NotificationSettings) =>
  apiPatch<NotificationSettings>('/notifications/settings', settings);
