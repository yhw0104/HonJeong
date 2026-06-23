import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  listMealRequests, createMealRequest, acceptMealRequest, declineMealRequest,
} from './api';

export function useReceivedRequests(status?: string) {
  return useQuery({
    queryKey: ['meal', 'list', 'received', status ?? 'all'],
    queryFn: () => listMealRequests('received', status),
  });
}

export function useSentRequests(status?: string) {
  return useQuery({
    queryKey: ['meal', 'list', 'sent', status ?? 'all'],
    queryFn: () => listMealRequests('sent', status),
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
