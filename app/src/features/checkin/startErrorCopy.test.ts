import { startErrorCopy } from './startErrorCopy';
import { ApiError } from '@/shared/api/client';

describe('startErrorCopy', () => {
  it('이미 모집/혼밥 중(CHECKIN_ALREADY_ACTIVE) 충돌이면 충돌 문구', () => {
    const copy = startErrorCopy(new ApiError(409, 'CHECKIN_ALREADY_ACTIVE', '이미 진행 중'));
    expect(copy.title).toBe('잠깐요');
    expect(copy.message).toBe('이미 다른 곳에서 모집/혼밥 중이에요. 먼저 끝내고 다시 시도해 주세요.');
  });

  it('네트워크 실패(백엔드 꺼짐, status 0)면 일반 실패 문구', () => {
    const copy = startErrorCopy(new ApiError(0, 'NETWORK_ERROR', '서버에 연결할 수 없습니다.'));
    expect(copy.title).toBe('앗');
    expect(copy.message).toBe('지금 시작하지 못했어요. 잠시 후 다시 시도해 주세요.');
  });

  it('그 외 서버 에러(예: 500)도 일반 실패 문구', () => {
    const copy = startErrorCopy(new ApiError(500, 'INTERNAL_ERROR', 'x'));
    expect(copy.title).toBe('앗');
    expect(copy.message).toBe('지금 시작하지 못했어요. 잠시 후 다시 시도해 주세요.');
  });

  it('ApiError가 아닌 에러도 일반 실패 문구', () => {
    const copy = startErrorCopy(new Error('boom'));
    expect(copy.title).toBe('앗');
    expect(copy.message).toBe('지금 시작하지 못했어요. 잠시 후 다시 시도해 주세요.');
  });
});
