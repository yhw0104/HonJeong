// API 주소에서 소켓 주소를 만든다. http는 ws로, https는 wss로 간다.
import { toWsUrl } from './wsUrl';

describe('toWsUrl', () => {
  it('https는 wss가 된다', () => {
    expect(toWsUrl('https://honjeong-api.com', 'T1')).toBe('wss://honjeong-api.com/ws?ticket=T1');
  });

  it('http는 ws가 된다 — 로컬 개발', () => {
    expect(toWsUrl('http://localhost:8080', 'T1')).toBe('ws://localhost:8080/ws?ticket=T1');
  });

  it('끝에 슬래시가 있어도 두 번 붙지 않는다', () => {
    expect(toWsUrl('https://honjeong-api.com/', 'T1')).toBe('wss://honjeong-api.com/ws?ticket=T1');
  });

  it('★티켓을 URL 인코딩한다 — base64url이 아닌 값이 와도 주소가 깨지지 않게', () => {
    expect(toWsUrl('https://a.com', 'a+b/c=')).toBe('wss://a.com/ws?ticket=a%2Bb%2Fc%3D');
  });
});
