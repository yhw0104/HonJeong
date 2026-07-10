import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { LIVE_REFETCH_MS } from '@/shared/realtime';
import {
  fetchNotifications, fetchUnreadCount, markNotificationRead, markAllNotificationsRead,
  fetchNotificationSettings, updateNotificationSettings, type NotificationSettings,
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

// 알림 설정은 라이브 폴링 불필요(사용자가 직접 바꿈). 토글 시 낙관적 업데이트로 즉시 반영.
export function useNotificationSettings() {
  return useQuery({
    queryKey: ['notifications', 'settings'],
    queryFn: fetchNotificationSettings,
  });
}

export function useUpdateNotificationSettings() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (next: NotificationSettings) => updateNotificationSettings(next),
    onMutate: async (next) => {
      await qc.cancelQueries({ queryKey: ['notifications', 'settings'] });
      const prev = qc.getQueryData<NotificationSettings>(['notifications', 'settings']);
      qc.setQueryData(['notifications', 'settings'], next);
      return { prev };
    },
    onError: (_err, _next, ctx) => {
      if (ctx?.prev) qc.setQueryData(['notifications', 'settings'], ctx.prev);
    },
    onSettled: () => qc.invalidateQueries({ queryKey: ['notifications', 'settings'] }),
  });
}
