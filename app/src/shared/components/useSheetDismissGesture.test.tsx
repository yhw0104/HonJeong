// 시트를 아래로 끌어 닫는 판단. 실기 테스트에서 "너무 살짝만 내려도 바로 닫힌다"는
// 지적을 받아 고친 규칙이라, 그 지적이 되살아나지 않게 여기서 고정한다.
import React from 'react';
import TestRenderer, { act } from 'react-test-renderer';
import { Animated } from 'react-native';
import { shouldDismissSheet, closeDuration, useSheetDismissGesture } from './useSheetDismissGesture';

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

/** Animated 값을 읽는다(내부 API지만 값 확인에는 이것뿐이다). */
const valueOf = (v: Animated.Value) => (v as unknown as { __getValue(): number }).__getValue();

/**
 * 닫는 애니메이션을 '즉시 끝난 것'으로 바꿔 끼운다 — 끝났을 때의 값(toValue)과
 * 완료 콜백만 실제와 같은 순서로 남긴다. 여기서 볼 것은 시간이 아니라
 * "onClose가 불리는 그 순간 시트가 어디에 있는가"뿐이다(시간은 closeDuration의 몫).
 */
function stubTiming() {
  return jest.spyOn(Animated, 'timing').mockImplementation(((value: Animated.Value, config: { toValue: number }) => ({
    start: (cb?: (r: { finished: boolean }) => void) => {
      value.setValue(config.toValue);
      cb?.({ finished: true });
    },
    stop: () => {},
    reset: () => {},
  })) as unknown as typeof Animated.timing);
}

function setup(height = 400) {
  const api = { current: null as ReturnType<typeof useSheetDismissGesture> | null };
  /** onClose가 불린 순간의 translateY 값. 이 순간의 위치가 화면에 비칠 수 있다. */
  const closedAt: number[] = [];
  function Probe({ open }: { open: boolean }) {
    const d = useSheetDismissGesture(open, () => closedAt.push(valueOf(d.translateY)));
    api.current = d;
    return null;
  }
  let r!: TestRenderer.ReactTestRenderer;
  act(() => { r = TestRenderer.create(<Probe open />); });
  act(() => { api.current!.onLayout({ nativeEvent: { layout: { height } } } as never); });
  return {
    api,
    closedAt,
    value: () => valueOf(api.current!.translateY),
    setOpen: (open: boolean) => act(() => { r.update(<Probe open={open} />); }),
  };
}

describe('닫고 다시 열기', () => {
  let timing: ReturnType<typeof stubTiming>;
  beforeEach(() => { timing = stubTiming(); });
  afterEach(() => { timing.mockRestore(); });

  it('★onClose를 부를 때 시트는 아직 화면 밖에 있다 — 원점으로 되돌린 모습이 비치면 안 된다', () => {
    // 실기 지적: "아래로 스와이프하면 잠깐 반짝하면서 목록이 나왔다 사라진다".
    // 부모가 시트를 걷어내는 건 다음 렌더라, 그 전에 값을 0으로 돌리면 원위치의 시트가 그려진다.
    const h = setup(400);
    act(() => { h.api.current!.requestClose(); });
    expect(h.closedAt).toEqual([400]);
  });

  it('★다시 열면 원점에서 시작한다 — 스크림만 깔리고 시트가 안 보이면 안 된다', () => {
    // 훅은 화면(MapHome)에 남고 시트만 언마운트되므로 Animated 값이 내려간 채 살아남는다.
    const h = setup(400);
    act(() => { h.api.current!.requestClose(); });
    h.setOpen(false);
    h.setOpen(true);
    expect(h.value()).toBe(0);
  });

  it('닫는 도중 또 닫으라고 해도 onClose는 한 번만 불린다', () => {
    const h = setup(400);
    act(() => {
      h.api.current!.requestClose();
      h.api.current!.requestClose();
    });
    expect(h.closedAt).toHaveLength(1);
  });
});
