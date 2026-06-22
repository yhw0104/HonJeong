import { useQuery } from '@tanstack/react-query';
import { fetchMyProfile } from './api';

/** 내 프로필(GET /users/me). 위치 폴백용 저장 동네 좌표 등에 쓴다. */
export function useMyProfile() {
  return useQuery({ queryKey: ['users', 'me'], queryFn: fetchMyProfile });
}
