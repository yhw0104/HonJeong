import { ApiError } from '@/shared/api/client';
import type { MealRequestStatus } from './api';

/** 보낸 신청 상태 → 사용자 표시 라벨. */
export function mealStatusLabel(status: MealRequestStatus): string {
  switch (status) {
    case 'ACCEPTED':
      return '수락됨';
    case 'DECLINED':
      return '거절됨';
    case 'PENDING':
    default:
      return '응답 대기 중';
  }
}

const ERROR_COPY: Record<string, string> = {
  TARGET_CHECKIN_NOT_AVAILABLE: '상대가 혼밥을 종료했어요.',
  MEALREQUEST_SELF: '본인에게는 신청할 수 없어요.',
  MEALREQUEST_OPT_OUT: '이 분은 같이먹기 신청을 받지 않아요.',
  MEALREQUEST_DUPLICATE: '이미 신청한 상대예요.',
  MEALREQUEST_ALREADY_RESPONDED: '이미 처리된 신청이에요.',
  NETWORK_ERROR: '연결을 확인해 주세요.',
};

/** 신청/응답 실패를 사용자 문구로 변환(알려진 코드 우선, 아니면 서버 메시지, 그 외 일반 문구). */
export function mealErrorMessage(err: unknown): string {
  if (err instanceof ApiError) return ERROR_COPY[err.code] ?? err.message;
  return '잠시 후 다시 시도해 주세요.';
}
