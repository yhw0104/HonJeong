import { searchOrigin } from './searchOrigin';

const seoul = { lat: 37.5665, lng: 126.978 };

describe('searchOrigin', () => {
  it('GPS면 그 좌표로 거리순 정렬한다', () => {
    expect(searchOrigin('gps', seoul)).toEqual(seoul);
  });

  it("내 동네('region')도 사용자가 직접 고른 값이라 쓴다", () => {
    expect(searchOrigin('region', seoul)).toEqual(seoul);
  });

  it('★기본 좌표는 쓰지 않는다 — 부산 사용자에게 서울 기준 거리순을 주게 된다', () => {
    // 'default'는 GPS도 내 동네 설정도 없을 때의 하드코딩된 연남동 좌표다.
    // null을 돌려주면 서버가 예전처럼 전국 이름순으로 응답한다 = 틀린 정렬을 하느니 안 한다.
    expect(searchOrigin('default', seoul)).toBeNull();
  });
});
