import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { LIVE_REFETCH_MS } from '@/shared/realtime';
import {
  fetchNotifications, fetchUnreadCount, markNotificationRead, markAllNotificationsRead,
} from './api';

// 알림은 상대 액션으로 생기므로 라이브 폴링(15s). 종 버튼 2곳은 같은 쿼리 키를 공유한다.
export function useNotifications() {
  return useQuery({
    queryKey: ['notifications', 'list'],
    queryFn: fetchNotifications,
    refetchInterval: LIVE_REFETCH_MS,
  });
}

export function useUnreadCount() {
  return useQuery({
    queryKey: ['notifications', 'unread-count'],
    queryFn: fetchUnreadCount,
    refetchInterval: LIVE_REFETCH_MS,
  });
}

export function useMarkRead() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => markNotificationRead(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['notifications'] }),
  });
}

export function useMarkAllRead() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => markAllNotificationsRead(),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['notifications'] }),
  });
}
