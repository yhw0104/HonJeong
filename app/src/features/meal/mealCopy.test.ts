import { ApiError } from '@/shared/api/client';
import { mealStatusLabelReceived, mealStatusLabelSent, mealErrorMessage } from './mealCopy';

describe('mealStatusLabelReceived (수신자 관점: 내가 처리)', () => {
  it('내가 직접 누른 것은 수락함/거절함, 자동 정리는 만료됨', () => {
    expect(mealStatusLabelReceived('PENDING')).toBe('응답 대기 중');
    expect(mealStatusLabelReceived('ACCEPTED')).toBe('수락함');
    expect(mealStatusLabelReceived('DECLINED')).toBe('거절함');
    expect(mealStatusLabelReceived('EXPIRED')).toBe('만료됨');
  });
});

describe('mealStatusLabelSent (신청자 관점: 상대가 처리)', () => {
  it('상대 행위는 수락됨/거절됨, 자동 정리는 만료됨', () => {
    expect(mealStatusLabelSent('PENDING')).toBe('응답 대기 중');
    expect(mealStatusLabelSent('ACCEPTED')).toBe('수락됨');
    expect(mealStatusLabelSent('DECLINED')).toBe('거절됨');
    expect(mealStatusLabelSent('EXPIRED')).toBe('만료됨');
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
