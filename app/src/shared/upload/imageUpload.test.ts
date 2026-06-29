import { extractUploadedUrl, remainingSlots } from './imageUpload';

describe('remainingSlots', () => {
  it('남은 슬롯 = max - current, 음수는 0', () => {
    expect(remainingSlots(2, 5)).toBe(3);
    expect(remainingSlots(5, 5)).toBe(0);
    expect(remainingSlots(7, 5)).toBe(0);
  });
});

describe('extractUploadedUrl', () => {
  it('files 응답 엔벨로프에서 url을 꺼낸다', () => {
    expect(extractUploadedUrl({ success: true, data: { url: 'http://x/a.jpg' } })).toBe('http://x/a.jpg');
  });
  it('url이 없으면 throw', () => {
    expect(() => extractUploadedUrl({ success: true, data: {} as any })).toThrow();
  });
});
