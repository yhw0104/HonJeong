import { pushTarget } from './target';

describe('pushTarget', () => {
  it('같이먹기 신청은 받은 신청 화면으로 간다', () => {
    expect(pushTarget({ type: 'MEAL_REQUEST_RECEIVED' })).toEqual({ screen: 'ReceivedRequests' });
  });

  it('같이먹기 수락은 홈으로 간다 — 같이 먹는 중 상태가 거기 보인다', () => {
    expect(pushTarget({ type: 'MEAL_REQUEST_ACCEPTED' })).toEqual({ screen: 'MainTabs' });
  });

  it('메이트 알림은 메이트 화면으로 간다', () => {
    expect(pushTarget({ type: 'MATE_REQUEST_RECEIVED' })).toEqual({ screen: 'Mates' });
  });

  it('뱃지는 뱃지 화면으로 간다', () => {
    expect(pushTarget({ type: 'BADGE_EARNED' })).toEqual({ screen: 'ChallengeBadges' });
  });

  it('채팅은 그 대화방으로 간다', () => {
    expect(pushTarget({ type: 'CHAT_MESSAGE', conversationId: '42' })).toEqual({
      screen: 'ChatRoom',
      params: { conversationId: 42 },
    });
  });

  it('채팅인데 대화방 id가 없으면 대화 목록 탭으로 간다', () => {
    expect(pushTarget({ type: 'CHAT_MESSAGE' })).toEqual({
      screen: 'MainTabs',
      params: { screen: 'Chat' },
    });
  });

  it('모르는 종류면 이동하지 않는다 — 서버가 새 종류를 보내도 앱이 깨지지 않게', () => {
    expect(pushTarget({ type: 'SOMETHING_NEW' })).toBeNull();
  });
});
