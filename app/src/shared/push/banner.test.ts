import { bannerIcon, shouldShowBanner } from './banner';

const chat = (conversationId?: string) => ({ type: 'CHAT_MESSAGE', conversationId });
const room = (conversationId: unknown) => ({ name: 'ChatRoom', params: { conversationId } });

describe('shouldShowBanner', () => {
  it('★ 보고 있는 대화방의 메시지는 띄우지 않는다 — 읽고 있는 화면을 가리기만 한다', () => {
    expect(shouldShowBanner(chat('7'), room(7))).toBe(false);
  });

  it('다른 대화방 메시지는 띄운다', () => {
    expect(shouldShowBanner(chat('7'), room(9))).toBe(true);
  });

  it('대화방 밖이면 띄운다', () => {
    expect(shouldShowBanner(chat('7'), { name: 'MainTabs' })).toBe(true);
  });

  it('채팅이 아닌 알림은 대화방 안이어도 띄운다 — 같이먹기·메이트는 그 방과 무관한 사건이다', () => {
    expect(shouldShowBanner({ type: 'MEAL_REQUEST_ACCEPTED' }, room(7))).toBe(true);
  });

  it('현재 화면을 모르면 띄운다 — 놓친 알림이 잘못 뜬 배너보다 나쁘다', () => {
    expect(shouldShowBanner(chat('7'), null)).toBe(true);
  });

  it('대화방 id가 없으면 띄운다 — 어느 방인지 모르면 숨길 근거가 없다', () => {
    expect(shouldShowBanner(chat(undefined), room(7))).toBe(true);
    expect(shouldShowBanner(chat('7'), { name: 'ChatRoom', params: {} })).toBe(true);
  });

  it('id 타입이 달라도 같은 방으로 본다 — data는 문자열, 라우트 파라미터는 숫자다', () => {
    // FCM data는 항상 문자열이고(FcmPushSender.putData) 네비게이션 파라미터는 숫자로 넘긴다.
    // 이걸 안 맞추면 규칙이 조용히 무력해진다 — '7' !== 7이라 늘 띄우게 된다.
    expect(shouldShowBanner(chat('7'), room(7))).toBe(false);
  });
});

describe('bannerIcon', () => {
  it('알림함과 같은 아이콘 규칙을 쓴다', () => {
    expect(bannerIcon('MEAL_REQUEST_RECEIVED')).toBe('rice');
    expect(bannerIcon('MATE_REQUEST_ACCEPTED')).toBe('friends');
    expect(bannerIcon('BADGE_EARNED')).toBe('badge');
  });

  it('채팅은 대화 아이콘', () => {
    expect(bannerIcon('CHAT_MESSAGE')).toBe('chat');
  });
});
