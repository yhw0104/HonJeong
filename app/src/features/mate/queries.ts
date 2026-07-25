import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  fetchMyMates, deleteMate, searchUsers, fetchPublicProfile,
  listMateRequests, sendMateRequest, acceptMateRequest, declineMateRequest, cancelMateRequest,
} from './api';
import { MIN_SEARCH_LEN } from '@/shared/search';

export function useMates() {
  return useQuery({ queryKey: ['mate', 'list'], queryFn: fetchMyMates });
}

export function useReceivedMateRequests(status?: string) {
  return useQuery({
    queryKey: ['mate', 'requests', 'received', status ?? 'all'],
    queryFn: () => listMateRequests('received', status),
  });
}

export function useSentMateRequests(status?: string) {
  return useQuery({
    queryKey: ['mate', 'requests', 'sent', status ?? 'all'],
    queryFn: () => listMateRequests('sent', status),
  });
}

export function useSearchUsers(nickname: string) {
  return useQuery({
    queryKey: ['mate', 'search', nickname],
    queryFn: () => searchUsers(nickname),
    enabled: nickname.trim().length >= MIN_SEARCH_LEN,
  });
}

export function useUserProfile(userId: number) {
  return useQuery({
    queryKey: ['mate', 'profile', userId],
    queryFn: () => fetchPublicProfile(userId),
  });
}

function useMateMutation<V>(fn: (v: V) => Promise<unknown>) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: fn,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['mate'] });
      // 메이트 수락/해제로 메이트 수가 바뀌므로 더보기·프로필 통계(activity-summary)도 갱신.
      qc.invalidateQueries({ queryKey: ['users', 'me', 'activity-summary'] });
    },
  });
}

export function useSendMateRequest() {
  return useMateMutation((toUserId: number) => sendMateRequest(toUserId));
}
export function useAcceptMateRequest() {
  return useMateMutation((id: number) => acceptMateRequest(id));
}
export function useDeclineMateRequest() {
  return useMateMutation((id: number) => declineMateRequest(id));
}
export function useCancelMateRequest() {
  return useMateMutation((id: number) => cancelMateRequest(id));
}
export function useDeleteMate() {
  return useMateMutation((mateUserId: number) => deleteMate(mateUserId));
}
