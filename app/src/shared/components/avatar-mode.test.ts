import { avatarMode } from './avatar-mode';

describe('avatarMode', () => {
  it('uri가 있으면 image', () => {
    expect(avatarMode('http://x/a.jpg')).toBe('image');
  });
  it('uri가 없으면 앱 아이콘 폴백', () => {
    expect(avatarMode(null)).toBe('fallback');
    expect(avatarMode(undefined)).toBe('fallback');
    expect(avatarMode('')).toBe('fallback');
  });
});
