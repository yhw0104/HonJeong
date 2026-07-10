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

test('본인 체크인 식당은 식당을 남기되 인원에서 본인만 뺀다(activeCount-1)', () => {
  const got = nearbyDiningPlaces([item(1, 100, 3), item(2, 200, 1)], 5, 1);
  expect(got.map((p) => [p.placeId, p.activeCount])).toEqual([
    [1, 2], // 3명 중 본인 제외 → 2명(다른 사람은 그대로 보임)
    [2, 1],
  ]);
});

test('본인만 있던 식당(activeCount 1)은 본인 제외 후 0명이라 사라진다', () => {
  const got = nearbyDiningPlaces([item(1, 100, 1), item(2, 200, 2)], 5, 1);
  expect(got.map((p) => p.placeId)).toEqual([2]);
});

test('selfPlaceId가 없으면(null/미지정) 인원을 그대로 둔다', () => {
  const items = [item(1, 100, 1), item(2, 200, 1)];
  expect(nearbyDiningPlaces(items, 5).map((p) => p.placeId)).toEqual([1, 2]);
  expect(nearbyDiningPlaces(items, 5, null).map((p) => p.placeId)).toEqual([1, 2]);
});

test('입력 배열·객체를 변형하지 않는다', () => {
  const items = [item(1, 300, 2), item(2, 100, 1)];
  nearbyDiningPlaces(items, 5, 1);
  expect(items.map((p) => p.placeId)).toEqual([1, 2]); // 원본 순서 그대로
  expect(items[0].activeCount).toBe(2); // 원본 인원은 감소하지 않음
});
