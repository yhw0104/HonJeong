import { totalUnread, messagePreview, isClosed } from './chatFormat';

describe('chatFormat', () => {
  it('totalUnread는 대화들의 안읽음 합', () => {
    expect(totalUnread([{ unreadCount: 2 } as any, { unreadCount: 3 } as any])).toBe(5);
    expect(totalUnread([])).toBe(0);
  });
  it('messagePreview는 이미지면 "사진"', () => {
    expect(messagePreview({ type: 'IMAGE', text: null } as any)).toBe('사진');
    expect(messagePreview({ type: 'TEXT', text: '곧 도착' } as any)).toBe('곧 도착');
    expect(messagePreview(null)).toBe('');
  });
  it('isClosed', () => {
    expect(isClosed('CLOSED')).toBe(true);
    expect(isClosed('ACTIVE')).toBe(false);
  });
});
