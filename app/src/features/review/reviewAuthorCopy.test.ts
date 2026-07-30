import { reviewAuthorUnavailableMessage } from './reviewAuthorCopy';

describe('reviewAuthorUnavailableMessage — 프로필로 갈 수 없는 리뷰 작성자 안내', () => {
  it('탈퇴한 작성자', () => {
    expect(reviewAuthorUnavailableMessage('WITHDRAWN')).toBe('탈퇴한 사용자입니다.');
  });
  it('정지된 작성자', () => {
    expect(reviewAuthorUnavailableMessage('SUSPENDED')).toBe('정지된 사용자입니다.');
  });
  it('그 외(UNKNOWN)는 이유를 지어내지 않는다', () => {
    expect(reviewAuthorUnavailableMessage('UNKNOWN')).toBe('프로필을 볼 수 없는 사용자입니다.');
  });
  it('서버가 이유를 주지 않아도 문구가 비지 않는다', () => {
    expect(reviewAuthorUnavailableMessage(null)).toBe('프로필을 볼 수 없는 사용자입니다.');
  });
});
