import { notificationMessage, notificationTarget, notificationIcon } from './copy';

describe('notificationMessage', () => {
  it('같이먹기 신청 받음', () =>
    expect(notificationMessage('MEAL_REQUEST_RECEIVED', '이나현')).toBe('이나현님이 같이 먹기를 신청했어요'));
  it('같이먹기 수락됨', () =>
    expect(notificationMessage('MEAL_REQUEST_ACCEPTED', '이나현')).toBe('이나현님이 같이 먹기를 수락했어요'));
  it('같이먹기 약속 취소됨(노쇼/취소)', () =>
    expect(notificationMessage('MEAL_MATCH_CANCELLED', '이나현')).toBe('이나현님이 같이 먹기 약속을 취소했어요'));
  it('메이트 신청 받음', () =>
    expect(notificationMessage('MATE_REQUEST_RECEIVED', '이나현')).toBe('이나현님이 메이트를 신청했어요'));
  it('메이트 수락됨', () =>
    expect(notificationMessage('MATE_REQUEST_ACCEPTED', '이나현')).toBe('이나현님이 메이트를 수락했어요'));
  it('닉네임 없으면 누군가', () =>
    expect(notificationMessage('MEAL_REQUEST_RECEIVED', null)).toBe('누군가님이 같이 먹기를 신청했어요'));
});

describe('notificationTarget', () => {
  it('같이먹기 받음 → 받은 신청', () => expect(notificationTarget('MEAL_REQUEST_RECEIVED')).toBe('ReceivedRequests'));
  it('같이먹기 수락 → 홈', () => expect(notificationTarget('MEAL_REQUEST_ACCEPTED')).toBe('MainTabs'));
  it('같이먹기 취소 → 홈', () => expect(notificationTarget('MEAL_MATCH_CANCELLED')).toBe('MainTabs'));
  it('메이트 받음/수락 → 메이트', () => {
    expect(notificationTarget('MATE_REQUEST_RECEIVED')).toBe('Mates');
    expect(notificationTarget('MATE_REQUEST_ACCEPTED')).toBe('Mates');
  });
});

describe('notificationIcon', () => {
  it('같이먹기 → 밥 아이콘', () => {
    expect(notificationIcon('MEAL_REQUEST_RECEIVED')).toBe('rice');
    expect(notificationIcon('MEAL_MATCH_CANCELLED')).toBe('rice');
  });
  it('메이트 → 친구 아이콘', () => {
    expect(notificationIcon('MATE_REQUEST_RECEIVED')).toBe('friends');
    expect(notificationIcon('MATE_REQUEST_ACCEPTED')).toBe('friends');
  });
});
