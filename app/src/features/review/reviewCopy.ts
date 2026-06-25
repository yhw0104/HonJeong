import { ApiError } from '@/shared/api/client';

const ERROR_COPY: Record<string, string> = {
  PLACE_NOT_FOUND: '식당 정보를 찾을 수 없어요.',
  FORBIDDEN: '권한이 없어요.',
  INVALID_INPUT: '입력을 다시 확인해 주세요.',
  NETWORK_ERROR: '연결을 확인해 주세요.',
};

export function reviewErrorMessage(err: unknown): string {
  if (err instanceof ApiError) {
    return ERROR_COPY[err.code] ?? err.message;
  }
  return '잠시 후 다시 시도해 주세요.';
}
