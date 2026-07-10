import { nearbyDiningPlaces } from './nearbyDining';
import type { PlaceNearbyItem } from './api';

const item = (placeId: number, distanceMeters: number, activeCount: number): PlaceNearbyItem => ({
  placeId,
  name: `p${placeId}`,
  category: null,
  roadAddress: null,
  latitude: 0,
  longitude: 0,
  distanceMeters,
  activeCount,
});

test('혼밥 인원(activeCount)이 0인 곳은 제외한다', () => {
  const got = nearbyDiningPlaces([item(1, 100, 0), item(2, 200, 3)], 5);
  expect(got.map((p) => p.placeId)).toEqual([2]);
});

test('가까운 순(거리 오름차순)으로 정렬한다', () => {
  const got = nearbyDiningPlaces([item(1, 300, 1), item(2, 100, 1), item(3, 200, 1)], 5);
  expect(got.map((p) => p.placeId)).toEqual([2, 3, 1]);
});

test('최대 limit개까지만, 가까운 순으로 자른다', () => {
  const items = [item(1, 500, 1), item(2, 100, 1), item(3, 300, 1), item(4, 200, 1)];
  const got = nearbyDiningPlaces(items, 2);
  expect(got.map((p) => p.placeId)).toEqual([2, 4]);
});

test('입력 배열을 변형하지 않는다', () => {
  const items = [item(1, 300, 1), item(2, 100, 1)];
  nearbyDiningPlaces(items, 5);
  expect(items.map((p) => p.placeId)).toEqual([1, 2]); // 원본 순서 그대로
});
