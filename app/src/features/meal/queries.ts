import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { LIVE_REFETCH_MS } from '@/shared/realtime';
import {
  listMealRequests, createMealRequest, acceptMealRequest, declineMealRequest, withdrawMealRequest,
} from './api';

// 상대가 수락/거절하면 내 목록(발신자 화면)에 곧 반영돼야 함 → 라이브 폴링.
export function useReceivedRequests(status?: string) {
  return useQuery({
    queryKey: ['meal', 'list', 'received', status ?? 'all'],
    queryFn: () => listMealRequests('received', status),
    refetchInterval: LIVE_REFETCH_MS,
  });
}

export function useSentRequests(status?: string) {
  return useQuery({
    queryKey: ['meal', 'list', 'sent', status ?? 'all'],
    queryFn: () => listMealRequests('sent', status),
    refetchInterval: LIVE_REFETCH_MS,
  });
}

export function useCreateMealRequest() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ toCheckInId, message }: { toCheckInId: number; message?: string }) =>
      createMealRequest(toCheckInId, message),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['meal'] }),
  });
}

export function useAcceptMealRequest() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => acceptMealRequest(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['meal'] }),
  });
}

export function useDeclineMealRequest() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => declineMealRequest(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['meal'] }),
  });
}

// 신청자가 보낸 PENDING 신청을 철회 → 보낸 목록 갱신.
export function useWithdrawMealRequest() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => withdrawMealRequest(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['meal'] }),
  });
}
