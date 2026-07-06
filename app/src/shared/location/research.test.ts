import { RESEARCH_THRESHOLD_M, shouldOfferResearch } from './research';

// 연남동 부근 기준점. 위도 0.001도 ≈ 111m.
const anchor = { lat: 37.5665, lng: 126.9236 };

test('기준점에서 200m 미만이면 재검색을 권하지 않는다', () => {
  const near = { lat: anchor.lat + 0.001, lng: anchor.lng }; // ≈111m
  expect(shouldOfferResearch(near, anchor, 'gps')).toBe(false);
});

test('기준점에서 200m 이상 벗어나면 재검색을 권한다(경계 포함)', () => {
  const far = { lat: anchor.lat + 0.002, lng: anchor.lng }; // ≈222m
  expect(shouldOfferResearch(far, anchor, 'gps')).toBe(true);
});

test('GPS가 아닌 폴백 좌표면 얼마나 멀든 false — 이동 개념이 없다', () => {
  const far = { lat: anchor.lat + 0.01, lng: anchor.lng }; // ≈1.1km
  expect(shouldOfferResearch(far, anchor, 'region')).toBe(false);
  expect(shouldOfferResearch(far, anchor, 'default')).toBe(false);
});

test('문턱 상수는 200m', () => {
  expect(RESEARCH_THRESHOLD_M).toBe(200);
});
