import { formatDistance, formatElapsed, shortAddress } from './format';

describe('formatDistance', () => {
  it('1000m 미만은 m', () => expect(formatDistance(120)).toBe('120m'));
  it('1000m 이상은 km 소수1', () => expect(formatDistance(1500)).toBe('1.5km'));
  it('정확히 1000m는 1.0km', () => expect(formatDistance(1000)).toBe('1.0km'));
});

describe('formatElapsed', () => {
  it('60분 미만은 분', () => expect(formatElapsed(25)).toBe('25분째'));
  it('60분 이상은 시간', () => expect(formatElapsed(90)).toBe('1시간째'));
});

describe('shortAddress', () => {
  it('시·도 접두사를 떼어 토큰 경계로 줄인다', () => {
    expect(shortAddress('서울특별시 마포구 성미산로 161-4')).toBe('마포구 성미산로 161-4');
    expect(shortAddress('경기도 성남시 분당구 판교로 230')).toBe('성남시 분당구 판교로 230');
    expect(shortAddress('제주특별자치도 제주시 첨단로 242')).toBe('제주시 첨단로 242');
  });
  it('시·도가 없으면 원문 그대로', () => {
    expect(shortAddress('마포구 동교로 38-12')).toBe('마포구 동교로 38-12');
    expect(shortAddress('주소 정보 없음')).toBe('주소 정보 없음');
  });
  it('공백 정리', () => expect(shortAddress('  서울 마포구 성미산로  ')).toBe('마포구 성미산로'));
});
