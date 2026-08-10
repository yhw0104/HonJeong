// 시트를 아래로 끌어 닫는 판단. 실기 테스트에서 "너무 살짝만 내려도 바로 닫힌다"는
// 지적을 받아 고친 규칙이라, 그 지적이 되살아나지 않게 여기서 고정한다.
import { shouldDismissSheet, closeDuration } from './useSheetDismissGesture';

describe('shouldDismissSheet', () => {
  describe('거리로 닫기', () => {
    it('충분히 끌어내리면 속도가 0이어도 닫는다', () => {
      expect(shouldDismissSheet(120, 0)).toBe(true);
      expect(shouldDismissSheet(300, 0)).toBe(true);
    });

    it('덜 끌었고 천천히 놓으면 닫지 않는다', () => {
      expect(shouldDismissSheet(119, 0.2)).toBe(false);
    });
  });

  describe('튕겨서 닫기', () => {
    it('★짧고 빠른 움직임만으로는 닫지 않는다 — 실기에서 지적된 바로 그 경우', () => {
      // 손잡이를 툭 건드린 정도. 예전 규칙(vy > 0.7만 보면 닫음)에서는 여기서 닫혔다.
      expect(shouldDismissSheet(20, 2.0)).toBe(false);
      expect(shouldDismissSheet(47, 5.0)).toBe(false);
    });

    it('충분히 내려온 상태에서 세게 튕기면 거리가 모자라도 닫는다', () => {
      expect(shouldDismissSheet(48, 1.2)).toBe(true);
      expect(shouldDismissSheet(60, 3.0)).toBe(true);
    });

    it('충분히 내려왔어도 느리게 놓으면 닫지 않는다 — 위치만 옮겨보는 손짓', () => {
      expect(shouldDismissSheet(60, 1.19)).toBe(false);
    });
  });

  describe('위로 끄는 방향', () => {
    it('위로 끌거나 위로 튕기면 닫지 않는다', () => {
      expect(shouldDismissSheet(-100, -3)).toBe(false);
      expect(shouldDismissSheet(0, -3)).toBe(false);
    });
  });
});

describe('closeDuration', () => {
  it('남은 거리에 비례한다 — 손을 뗀 흐름을 이어 미끄러진다', () => {
    expect(closeDuration(440)).toBeCloseTo(200);
    expect(closeDuration(330)).toBeCloseTo(150);
  });

  it('거의 다 내려온 시트가 굼뜨지 않게 하한을 둔다', () => {
    expect(closeDuration(0)).toBe(130);
    expect(closeDuration(20)).toBe(130);
  });

  it('키가 큰 시트도 늘어지지 않게 상한을 둔다', () => {
    expect(closeDuration(2000)).toBe(300);
  });
});
