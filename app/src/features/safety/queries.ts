import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { blockUser, createReport, fetchBlockedUsers, fetchMyReports, unblockUser } from './api';

export function useBlockedUsers() {
  return useQuery({ queryKey: ['safety', 'blocks'], queryFn: fetchBlockedUsers });
}

export function useMyReports() {
  return useQuery({ queryKey: ['safety', 'reports'], queryFn: fetchMyReports });
}

/** 차단/해제는 상호 은닉 범위(혼밥러·신청·메이트·리뷰·TOGETHER)가 넓어 관련 캐시를 모두 갱신한다. */
function useBlockMutation(fn: (targetUserId: number) => Promise<unknown>) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: fn,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['safety'] });
      qc.invalidateQueries({ queryKey: ['place'] });
      qc.invalidateQueries({ queryKey: ['meal'] });
      qc.invalidateQueries({ queryKey: ['mate'] });
      qc.invalidateQueries({ queryKey: ['checkin'] });
    },
  });
}

export function useBlockUser() {
  return useBlockMutation(blockUser);
}
export function useUnblockUser() {
  return useBlockMutation(unblockUser);
}

export function useCreateReport() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: createReport,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['safety', 'reports'] }),
  });
}
