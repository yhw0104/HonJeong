import { pushInvalidationKeys } from './invalidate';

describe('pushInvalidationKeys', () => {
  it('모든 푸시는 알림함을 갱신한다', () => {
    expect(pushInvalidationKeys('BADGE_EARNED')).toContainEqual(['notifications']);
  });

  it('채팅은 대화 캐시를 갱신한다', () => {
    expect(pushInvalidationKeys('CHAT_MESSAGE')).toContainEqual(['chat']);
  });

  it('채팅은 알림함을 갱신하지 않는다 — 채팅은 알림함에 쌓이지 않아 새로 받을 행이 없다', () => {
    expect(pushInvalidationKeys('CHAT_MESSAGE')).not.toContainEqual(['notifications']);
  });

  it('같이먹기 신청은 meal 캐시를 갱신한다', () => {
    expect(pushInvalidationKeys('MEAL_REQUEST_RECEIVED')).toContainEqual(['meal']);
  });

  it('메이트 알림은 mate 캐시를 갱신한다', () => {
    expect(pushInvalidationKeys('MATE_REQUEST_ACCEPTED')).toContainEqual(['mate']);
  });

  it('모르는 종류여도 알림함은 갱신한다 — 최소한 종 뱃지는 맞는다', () => {
    expect(pushInvalidationKeys('SOMETHING_NEW')).toEqual([['notifications']]);
  });
});
