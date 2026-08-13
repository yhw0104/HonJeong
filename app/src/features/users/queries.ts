import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { fetchMyProfile, updateMyProfile, fetchActivitySummary } from './api';

/**
 * 내 프로필(GET /users/me). 위치 폴백·프로필 화면에 쓴다.
 *
 * @param options.enabled false면 조회 자체를 하지 않는다(기본 true — 기존 호출부는 그대로 동작).
 *   비로그인 상태에서 이 쿼리를 실행하면 401 → refresh 실패 → 세션 만료 캐스케이드로 이어질 수
 *   있는 화면(예: 앱 전역에 마운트되는 컴포넌트)에서 로그인 여부에 따라 끈다.
 */
export function useMyProfile(options?: { enabled?: boolean }) {
  return useQuery({ queryKey: ['users', 'me'], queryFn: fetchMyProfile, enabled: options?.enabled ?? true });
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
