import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { LIVE_REFETCH_MS } from '@/shared/realtime';
import { invalidateCheckInLoop } from '@/features/checkin/queries';
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

// 수락은 신청 상태만 바꾸는 게 아니다 — 내 체크인이 SEEKING→TOGETHER로 전이하고 대화가 개설된다
// (MealRequestService.accept). 신청 목록만 무효화하면 상태바가 낡은 채로 남으므로 체크인 루프와
// 대화 목록까지 함께 무효화한다(체크인 루프에 ['meal']이 포함돼 있다).
export function useAcceptMealRequest() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: number) => acceptMealRequest(id),
    onSuccess: () => {
      invalidateCheckInLoop(qc);
      qc.invalidateQueries({ queryKey: ['chat'] });
    },
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
