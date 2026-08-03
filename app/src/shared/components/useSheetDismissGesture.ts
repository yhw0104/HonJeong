// 바텀시트를 아래로 끌어 닫는 제스처. EndHonbabSheet·DirectionsSheet가 공유한다.
//
// 왜 필요한가: 두 시트 모두 위쪽에 손잡이(handle) 막대를 그려 "당길 수 있다"고 신호를 주면서
// 실제로는 스크림 탭과 X 버튼으로만 닫혔다. 보이는 것과 동작이 어긋나 실기 테스트에서 지적됐다.
//
// ★핸들러를 시트 전체가 아니라 **헤더 영역에만** 붙이도록 설계했다. EndHonbabSheet의
// '밀어서 완료'(SlideToConfirm)가 가로 드래그라, 시트 전체가 제스처를 잡으면 그 조작을 삼킬 수 있다.
// 세로 우세 조건(|dy| > |dx|)도 함께 걸어 이중으로 막는다.
import { useEffect, useRef } from 'react';
import { Animated, PanResponder } from 'react-native';

/** 이만큼 아래로 끌면 닫는다(px). 손이 미끄러진 정도로는 안 닫히게 여유를 둔다. */
const DISMISS_DISTANCE = 90;
/** 짧게 끌어도 아래로 튕기면 닫는다(px/ms) — 빠른 플릭 대응. */
const DISMISS_VELOCITY = 0.7;

/**
 * @param open 시트가 열려 있는지. false가 되면 위치를 원점으로 되돌린다.
 * @param onClose 닫기 요청 콜백.
 * @returns translateY - 시트에 줄 Animated 값 / panHandlers - 헤더 영역에 펼칠 핸들러
 */
export function useSheetDismissGesture(open: boolean, onClose: () => void) {
  const translateY = useRef(new Animated.Value(0)).current;
  // PanResponder는 한 번만 만들고 재사용하므로, 최신 onClose를 ref로 들고 본다
  // (매 렌더 새로 만들면 드래그 도중 핸들러가 갈아끼워져 제스처가 끊긴다).
  const closeRef = useRef(onClose);
  closeRef.current = onClose;

  // 닫히면 다음 열림을 위해 원점으로. 애니메이션 없이 즉시 되돌린다(닫히는 순간은 안 보인다).
  useEffect(() => {
    if (!open) translateY.setValue(0);
  }, [open, translateY]);

  const pan = useRef(
    PanResponder.create({
      // 탭은 통과시킨다 — X 버튼·시트 안 버튼이 정상 동작해야 한다.
      onStartShouldSetPanResponder: () => false,
      // 아래 방향의 세로 드래그일 때만 잡는다.
      onMoveShouldSetPanResponder: (_, g) => g.dy > 4 && Math.abs(g.dy) > Math.abs(g.dx),
      // 위로 끄는 건 무시(시트가 천장에 붙어 늘어나 보이지 않게).
      onPanResponderMove: (_, g) => {
        if (g.dy > 0) translateY.setValue(g.dy);
      },
      onPanResponderRelease: (_, g) => {
        if (g.dy > DISMISS_DISTANCE || g.vy > DISMISS_VELOCITY) {
          closeRef.current();
          return; // 위치 복원은 위 useEffect가 open=false를 보고 처리한다
        }
        Animated.spring(translateY, { toValue: 0, useNativeDriver: true, bounciness: 4 }).start();
      },
      // 제스처를 뺏겼을 때도 제자리로(시트가 반쯤 내려간 채 남지 않게).
      onPanResponderTerminate: () => {
        Animated.spring(translateY, { toValue: 0, useNativeDriver: true, bounciness: 4 }).start();
      },
    }),
  ).current;

  return { translateY, panHandlers: pan.panHandlers };
}
