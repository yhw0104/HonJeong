import type { Coord, LocationSource } from '@/shared/location/pickLocation';

/**
 * 검색 결과를 거리순으로 세울 기준 좌표를 고른다. null이면 서버가 전국 이름순으로 돌려준다.
 *
 * <p>★{@code 'default'}는 쓰지 않는다. 그건 GPS도 없고 내 동네 설정도 없을 때 쓰는 하드코딩된
 * 연남동 좌표라(pickLocation), 부산에 있는 사람에게 <b>서울 기준 거리순</b>을 주게 된다.
 * 그런 정렬은 틀렸다는 신호도 없이 조용히 엉뚱한 결과를 위로 올린다 — 정렬하지 않는 편이 낫다.
 *
 * <p>{@code 'region'}(내 동네)은 사용자가 직접 고른 값이라 쓴다. GPS만큼 정확하진 않아도
 * 사용자가 자기 생활권이라고 말한 곳이다.
 */
export function searchOrigin(source: LocationSource, coord: Coord): Coord | null {
  return source === 'gps' || source === 'region' ? coord : null;
}
