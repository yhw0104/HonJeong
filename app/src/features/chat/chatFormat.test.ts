import { totalUnread, messagePreview, isClosed, formatTime, readByPartner } from './chatFormat';

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
  it('formatTime은 ISO에서 HH:MM 추출', () => {
    expect(formatTime('2026-07-25T14:30:00')).toBe('14:30');
    expect(formatTime('2026-07-25T09:05:12.345')).toBe('09:05');
  });
  it('readByPartner는 상대 읽은 시각이 메시지 시각 이상이면 읽음', () => {
    expect(readByPartner('2026-07-25T14:30:00', '2026-07-25T14:31:00')).toBe(true);
    expect(readByPartner('2026-07-25T14:30:00', '2026-07-25T14:30:00')).toBe(true);
    expect(readByPartner('2026-07-25T14:30:00', '2026-07-25T14:29:59')).toBe(false);
    expect(readByPartner('2026-07-25T14:30:00', null)).toBe(false);
  });
});
