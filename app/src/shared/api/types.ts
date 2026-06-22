// 백엔드 PageResponse<T> 엔벨로프(client.ts가 data로 풀어준 형태).
export type Page<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
};
