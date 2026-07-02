import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  fetchMyMates, deleteMate, searchUsers, fetchPublicProfile,
  listMateRequests, sendMateRequest, acceptMateRequest, declineMateRequest, cancelMateRequest,
} from './api';

export function useMates() {
  return useQuery({ queryKey: ['mate', 'list'], queryFn: fetchMyMates });
}

export function useReceivedMateRequests(status?: string) {
  return useQuery({
    queryKey: ['mate', 'requests', 'received', status ?? 'all'],
    queryFn: () => listMateRequests('received', status),
  });
}

export function useSearchUsers(nickname: string) {
  return useQuery({
    queryKey: ['mate', 'search', nickname],
    queryFn: () => searchUsers(nickname),
    enabled: nickname.trim().length > 0,
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
    onSuccess: () => qc.invalidateQueries({ queryKey: ['mate'] }),
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
