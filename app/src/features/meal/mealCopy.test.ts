import { ApiError } from '@/shared/api/client';
import { mealStatusLabel, mealErrorMessage } from './mealCopy';

describe('mealStatusLabel', () => {
  it('상태별 한글 라벨', () => {
    expect(mealStatusLabel('PENDING')).toBe('응답 대기 중');
    expect(mealStatusLabel('ACCEPTED')).toBe('수락됨');
    expect(mealStatusLabel('DECLINED')).toBe('거절됨');
  });
});

describe('mealErrorMessage', () => {
  it('알려진 코드는 친절 카피로 매핑', () => {
    expect(mealErrorMessage(new ApiError(409, 'MEALREQUEST_DUPLICATE', 'x'))).toBe('이미 신청한 상대예요.');
    expect(mealErrorMessage(new ApiError(0, 'NETWORK_ERROR', 'x'))).toBe('연결을 확인해 주세요.');
  });
  it('모르는 코드는 서버 메시지로 폴백', () => {
    expect(mealErrorMessage(new ApiError(500, 'WHATEVER', '서버 메시지'))).toBe('서버 메시지');
  });
  it('ApiError가 아니면 일반 문구', () => {
    expect(mealErrorMessage(new Error('boom'))).toBe('잠시 후 다시 시도해 주세요.');
  });
});
