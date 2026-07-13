import { RESEARCH_THRESHOLD_M, shouldOfferResearch } from './research';

// 연남동 부근 기준점. 위도 0.001도 ≈ 111m.
const anchor = { lat: 37.5665, lng: 126.9236 };

test('지도 중심이 기준점에서 200m 미만이면 재검색을 권하지 않는다', () => {
  const near = { lat: anchor.lat + 0.001, lng: anchor.lng }; // ≈111m
  expect(shouldOfferResearch(near, anchor)).toBe(false);
});

test('지도 중심이 기준점에서 200m 이상 벗어나면 재검색을 권한다(경계 포함)', () => {
  const far = { lat: anchor.lat + 0.002, lng: anchor.lng }; // ≈222m
  expect(shouldOfferResearch(far, anchor)).toBe(true);
});

test('문턱 상수는 200m', () => {
  expect(RESEARCH_THRESHOLD_M).toBe(200);
});
