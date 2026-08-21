/** 홈 지도 하단 시트의 단계. 아래에서 위 순서다. */
export type SheetSnap = 'collapsed' | 'mid' | 'expanded';

/** 단계 순서. nextSnap이 이 배열 위에서 한 칸씩 움직인다. */
const ORDER: SheetSnap[] = ['collapsed', 'mid', 'expanded'];

/** 이만큼은 끌어야 단계를 옮긴다. 이보다 작으면 탭이나 손떨림으로 보고 제자리에 둔다. */
export const SNAP_THRESHOLD = 50;

/**
 * 드래그를 놓았을 때 갈 단계. **한 번에 한 칸씩만 움직인다.**
 *
 * <p>★예전에는 크게·빠르게 끌면(160px 또는 속도 1.2 초과) 가운데를 건너뛰고 끝까지 갔다.
 * "한 번에 지도를 다 열고 싶을 때 두 번 끌지 않게" 하려던 것인데, 실기에서 <b>중간 단계를
 * 지나치는 게 더 불편하다</b>는 지적을 받았다 — 목록을 조금만 보려고 내렸는데 접혀 버리고,
 * 지도를 조금만 보려고 올렸는데 다 펴졌다. 지금은 끄는 세기와 무관하게 항상 한 칸이다.
 *
 * @param cur 현재 단계
 * @param dy  드래그 거리(위로 끌면 음수 — React Native PanResponder 관례)
 */
export function nextSnap(cur: SheetSnap, dy: number): SheetSnap {
  const i = ORDER.indexOf(cur);
  if (dy < -SNAP_THRESHOLD) return ORDER[Math.min(i + 1, ORDER.length - 1)]; // 위로
  if (dy > SNAP_THRESHOLD) return ORDER[Math.max(i - 1, 0)];                 // 아래로
  return cur;
}
