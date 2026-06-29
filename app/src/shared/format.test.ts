import { formatDistance, formatElapsed, addressHead } from './format';

describe('formatDistance', () => {
  it('1000m 미만은 m', () => expect(formatDistance(120)).toBe('120m'));
  it('1000m 이상은 km 소수1', () => expect(formatDistance(1500)).toBe('1.5km'));
  it('정확히 1000m는 1.0km', () => expect(formatDistance(1000)).toBe('1.0km'));
});

describe('formatElapsed', () => {
  it('60분 미만은 분', () => expect(formatElapsed(25)).toBe('25분째'));
  it('60분 이상은 시간', () => expect(formatElapsed(90)).toBe('1시간째'));
});

describe('addressHead', () => {
  it('도로명 앞(시·도~시·군·구)까지만 반환', () => {
    expect(addressHead('서울특별시 마포구 성미산로 161-4')).toBe('서울특별시 마포구');
    expect(addressHead('경기도 성남시 분당구 판교로 230')).toBe('경기도 성남시 분당구');
    expect(addressHead('제주특별자치도 제주시 첨단로 242')).toBe('제주특별자치도 제주시');
  });
  it('시·도 없이 구로 시작해도 도로명 앞까지', () => {
    expect(addressHead('마포구 동교로 38-12')).toBe('마포구');
  });
  it('지번(번지)은 숫자 토큰 앞까지(동 포함)', () => {
    expect(addressHead('서울특별시 마포구 연남동 567-1')).toBe('서울특별시 마포구 연남동');
  });
  it('뗄 게 없으면 원문 그대로', () => {
    expect(addressHead('주소 정보 없음')).toBe('주소 정보 없음');
  });
});
