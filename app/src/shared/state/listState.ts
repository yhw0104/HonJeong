// listState — React Query 플래그를 리스트 표시 상태로 환원. 데이터가 있으면 항상 ready(리페치·에러여도 기존 목록 유지).
export type ListState = 'loading' | 'error' | 'empty' | 'ready';

export function listState(q: { isLoading: boolean; isError: boolean; count: number }): ListState {
  if (q.count > 0) return 'ready';
  if (q.isError) return 'error';
  if (q.isLoading) return 'loading';
  return 'empty';
}
