import { avatarMode } from './avatar-mode';

describe('avatarMode', () => {
  it('uri가 있으면 image', () => {
    expect(avatarMode('http://x/a.jpg', '혼')).toBe('image');
    expect(avatarMode('http://x/a.jpg', null)).toBe('image');
  });
  it('uri 없고 이름 있으면 initial', () => {
    expect(avatarMode(null, '혼밥러')).toBe('initial');
    expect(avatarMode(undefined, '혼')).toBe('initial');
  });
  it('uri도 이름도 없으면 default', () => {
    expect(avatarMode(null, null)).toBe('default');
    expect(avatarMode(undefined, undefined)).toBe('default');
    expect(avatarMode('', '')).toBe('default');
  });
});
