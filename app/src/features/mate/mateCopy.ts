import { ApiError } from '@/shared/api/client';

const ERROR_COPY: Record<string, string> = {
  MATE_SELF: '본인에게는 신청할 수 없어요.',
  MATE_ALREADY: '이미 메이트예요.',
  MATE_REQUEST_DUPLICATE: '이미 신청한 상대예요.',
  MATE_REQUEST_NOT_FOUND: '신청을 찾을 수 없어요.',
  MATE_REQUEST_ALREADY_RESPONDED: '이미 처리된 신청이에요.',
  MATE_NOT_FOUND: '메이트 관계를 찾을 수 없어요.',
  NETWORK_ERROR: '연결을 확인해 주세요.',
};

export function mateErrorMessage(err: unknown): string {
  if (err instanceof ApiError) return ERROR_COPY[err.code] ?? err.message;
  return '잠시 후 다시 시도해 주세요.';
}
