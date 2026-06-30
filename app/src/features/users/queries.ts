import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { fetchMyProfile, updateMyProfile, fetchActivitySummary } from './api';

/** 내 프로필(GET /users/me). 위치 폴백·프로필 화면에 쓴다. */
export function useMyProfile() {
  return useQuery({ queryKey: ['users', 'me'], queryFn: fetchMyProfile });
}

/** 내 활동요약(GET /users/me/activity-summary). 프로필 카드 통계 행에 쓴다. */
export function useActivitySummary() {
  return useQuery({ queryKey: ['users', 'me', 'activity-summary'], queryFn: fetchActivitySummary });
}

/** 내 프로필 수정(PATCH /users/me). 성공 시 내 프로필 캐시를 새로고침한다. */
export function useUpdateMyProfile() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: updateMyProfile,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['users', 'me'] });
    },
  });
}
