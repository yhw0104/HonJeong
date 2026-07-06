import { useQuery } from '@tanstack/react-query';
import { fetchNotices } from './api';

/** 공지 목록. 폴링 없음 — 화면 진입 시 조회. */
export function useNotices() {
  return useQuery({
    queryKey: ['notices'],
    queryFn: async () => (await fetchNotices()).notices,
  });
}
