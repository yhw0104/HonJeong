import { ApiError } from '@/shared/api/client';
import { reviewErrorMessage } from './reviewCopy';

describe('reviewErrorMessage', () => {
  it('알려진 코드는 친절 카피', () => {
    expect(reviewErrorMessage(new ApiError(400, 'INVALID_INPUT', 'x'))).toBe('입력을 다시 확인해 주세요.');
    expect(reviewErrorMessage(new ApiError(0, 'NETWORK_ERROR', 'x'))).toBe('연결을 확인해 주세요.');
  });
  it('모르는 코드는 서버 메시지 폴백', () => {
    expect(reviewErrorMessage(new ApiError(500, 'WHATEVER', '서버 메시지'))).toBe('서버 메시지');
  });
  it('ApiError가 아니면 일반 문구', () => {
    expect(reviewErrorMessage(new Error('boom'))).toBe('잠시 후 다시 시도해 주세요.');
  });
});
