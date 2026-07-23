import { useQuery } from '@tanstack/react-query';
import { fetchBadges } from './api';

/** 내 뱃지. 화면 진입 시 새로고침(라이브 아님 — refetchInterval 없음). */
export function useBadges() {
  return useQuery({ queryKey: ['users', 'me', 'badges'], queryFn: fetchBadges });
}
