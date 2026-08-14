// 바텀시트를 아래로 끌어 닫는 제스처. EndHonbabSheet·DirectionsSheet·MapHome의 두 시트가 공유한다.
//
// 왜 필요한가: 시트들이 위쪽에 손잡이(handle) 막대를 그려 "당길 수 있다"고 신호를 주면서
// 실제로는 스크림 탭과 X 버튼으로만 닫혔다. 보이는 것과 동작이 어긋나 실기 테스트에서 지적됐다.
//
// ★핸들러를 시트 전체가 아니라 **헤더 영역에만** 붙이도록 설계했다. EndHonbabSheet의
// '밀어서 완료'(SlideToConfirm)가 가로 드래그라, 시트 전체가 제스처를 잡으면 그 조작을 삼킬 수 있다.
// 세로 우세 조건(|dy| > |dx|)도 함께 걸어 이중으로 막는다.
//
// ★닫을 때는 **미끄러져 내려간 뒤에** 닫는다({@link useSheetDismissGesture}의 requestClose).
// 예전에는 손을 떼는 순간 부모가 시트를 언마운트해서 화면에서 툭 사라졌다 — iOS 기본 모달
// (presentation:'modal'인 같이먹기 신청·프로필 편집)이 아래로 미끄러지는 것과 달라 어색하다는
// 지적을 받았다. 그래서 시트를 화면 밖까지 내린 다음 onClose를 부른다.
import { useCallback, useRef } from 'react';
import { Animated, Dimensions, Easing, PanResponder, type LayoutChangeEvent } from 'react-native';

/** 이만큼 아래로 끌면 닫는다(px). 손이 미끄러진 정도로는 안 닫히게 여유를 둔다. */
const DISMISS_DISTANCE = 120;
/** 짧게 끌어도 아래로 세게 튕기면 닫는다(px/ms) — 빠른 플릭 대응. */
const DISMISS_VELOCITY = 1.2;
/** 튕겨서 닫으려면 최소 이만큼은 실제로 내려와 있어야 한다(px). */
const FLICK_MIN_DISTANCE = 48;
/** 이만큼 움직여야 제스처를 잡는다(px). 손가락이 스친 정도로는 시트가 안 따라오게. */
const CAPTURE_DISTANCE = 12;

/** 닫히며 미끄러지는 속도(px/ms). 남은 거리에 비례해 시간을 정해 손을 뗀 흐름을 잇는다. */
const CLOSE_SPEED = 2.2;
/** 미끄러지는 시간의 하한·상한(ms). 거의 다 내려온 시트가 굼뜨거나, 큰 시트가 늘어지지 않게. */
const CLOSE_MIN_MS = 130;
const CLOSE_MAX_MS = 300;

/**
 * 손을 뗐을 때 닫을 것인가. (순수)
 *
 * ★두 갈래를 **모두** 거리로 잠근다. 이전에는 속도만 넘으면 닫았는데(`vy > 0.7`),
 * 손잡이를 툭 건드리는 정도의 짧고 빠른 움직임이 그 값을 쉽게 넘어서
 * "살짝만 내려도 바로 닫힌다"는 지적을 받았다. 튕겨서 닫으려면 실제로
 * {@link FLICK_MIN_DISTANCE}만큼은 내려와 있어야 한다.
 *
 * ★`'worklet'`이 붙어 있는 이유: 이 규칙을 PhotoViewer도 쓴다. 거기는 Reanimated라 판정이
 * UI 스레드에서 돌아야 한다. 앱 안에서 "아래로 쓸어 닫기"의 문턱은 한 곳에만 있어야 한다 —
 * 시트와 사진 뷰어가 서로 다른 감각으로 닫히면 그게 곧 버그로 읽힌다.
 *
 * ★단위 주의: Reanimated의 `velocityY`는 **px/초**이고 이 함수는 **px/ms**를 받는다.
 * 호출부에서 1000으로 나눠 넘길 것(안 나누면 살짝만 스쳐도 닫힌다).
 *
 * @param dy 제스처를 잡은 지점부터 내려온 거리(px, 아래가 양수)
 * @param vy 놓는 순간의 세로 속도(px/ms, 아래가 양수)
 * @returns 닫아야 하면 true
 */
export function shouldDismissSheet(dy: number, vy: number): boolean {
  'worklet';
  if (dy >= DISMISS_DISTANCE) return true;
  return vy >= DISMISS_VELOCITY && dy >= FLICK_MIN_DISTANCE;
}

/**
 * 화면 밖까지 남은 거리를 미끄러지는 데 쓸 시간. (순수)
 *
 * 고정값을 쓰면 어색하다 — 이미 손으로 90% 끌어내린 시트가 300ms를 더 기어가고,
 * 손도 안 댄 큰 시트는 순식간에 사라진다. 남은 거리에 비례시키되 양끝만 잘라 둔다.
 *
 * @param remaining 화면 밖까지 남은 거리(px)
 * @returns 애니메이션 시간(ms)
 */
export function closeDuration(remaining: number): number {
  return Math.min(CLOSE_MAX_MS, Math.max(CLOSE_MIN_MS, remaining / CLOSE_SPEED));
}

/**
 * @param open 시트가 열려 있는지. true가 되는 순간 위치를 원점으로 되돌린다.
 * @param onClose 닫기 요청 콜백. **미끄러져 내려간 뒤에** 불린다.
 * @returns translateY - 시트에 줄 Animated 값 / panHandlers - 헤더 영역에 펼칠 핸들러 /
 *   onLayout - 시트(Animated.View)에 달 핸들러. 시트 높이를 알아야 딱 화면 밖까지만 내린다 /
 *   requestClose - 스크림 탭·X 버튼처럼 '닫아라'를 뜻하는 조작에 연결한다. 스와이프와 같은 모양으로 닫힌다
 */
export function useSheetDismissGesture(open: boolean, onClose: () => void) {
  const translateY = useRef(new Animated.Value(0)).current;
  // PanResponder는 한 번만 만들고 재사용하므로, 최신 콜백을 ref로 들고 본다
  // (매 렌더 새로 만들면 드래그 도중 핸들러가 갈아끼워져 제스처가 끊긴다).
  const closeRef = useRef(onClose);
  closeRef.current = onClose;
  /** 시트 높이(onLayout). 이만큼 내리면 화면 밖이다. 못 재면 화면 높이로 대신한다. */
  const heightRef = useRef(0);
  /** 지금 손가락을 따라 내려와 있는 거리. 남은 거리를 재는 데 쓴다. */
  const offsetRef = useRef(0);
  /** 닫는 애니메이션이 도는 중인지 — 그 사이 또 닫으라고 해도 onClose가 두 번 불리지 않게. */
  const closingRef = useRef(false);

  // ★원점으로 되돌리는 일은 **'열릴 때', 렌더 도중**에 한다. 두 가지가 다 조건이다.
  //
  // 왜 '열릴 때'인가: 닫는 애니메이션이 끝난 자리에서 값을 0으로 돌리면, 부모가 시트를
  // 걷어내는 건 그다음 렌더라 그사이 네이티브가 **원위치의 시트를 한 프레임 그린다** —
  // 2026-08-10 실기에서 "아래로 스와이프하면 잠깐 반짝하며 목록이 나왔다 사라진다"로 나온
  // 증상이다. 값은 내려간 채로 두고, 다음에 열릴 때 되돌리는 것이 맞다.
  //
  // 왜 렌더 도중인가: 시트는 열릴 때 비로소 마운트된다({picking && <시트/>}). useEffect는
  // 커밋 뒤에 돌기 때문에 그때 되돌리면 이미 '내려간 자리'에 한 번 그려진 뒤다. 렌더 중에
  // 되돌리면 시트 뷰가 만들어지는 순간부터 값이 0이라 그런 프레임 자체가 없다.
  //
  // 되돌리기를 아예 빼면 안 된다: 훅은 시트가 아니라 **화면**(MapHome)에 살아서 Animated 값이
  // 내려간 채 살아남는다. 그러면 다시 열었을 때 스크림만 깔리고 시트는 화면 밖에 그려진다.
  const wasOpen = useRef(open);
  if (open !== wasOpen.current) {
    wasOpen.current = open;
    if (open) {
      translateY.setValue(0);
      offsetRef.current = 0;
      closingRef.current = false;
    }
  }

  const onLayout = useCallback((e: LayoutChangeEvent) => {
    heightRef.current = e.nativeEvent.layout.height;
  }, []);

  const requestClose = useCallback(() => {
    if (closingRef.current) return;
    closingRef.current = true;
    const target = heightRef.current > 0 ? heightRef.current : Dimensions.get('window').height;
    Animated.timing(translateY, {
      toValue: target,
      duration: closeDuration(Math.max(target - offsetRef.current, 0)),
      easing: Easing.out(Easing.cubic),
      useNativeDriver: true,
      // 다 내려간 뒤에야 부모가 시트를 치운다. 중간에 끊겨도(finished=false) 부르는 게 맞다 —
      // 안 부르면 시트가 화면 밖에 내려간 채 '열린 상태'로 남는다.
      //
      // ★여기서 원점으로 되돌리지 않는다 — 되돌리면 부모가 시트를 걷어내기 전에 원위치의
      // 시트가 한 프레임 비친다. 되돌리기는 다음에 열릴 때 한다(위 wasOpen 참고).
    }).start(() => closeRef.current());
  }, [translateY]);

  const closeAction = useRef(requestClose);
  closeAction.current = requestClose;

  const springBack = useCallback(() => {
    offsetRef.current = 0;
    Animated.spring(translateY, { toValue: 0, useNativeDriver: true, bounciness: 4 }).start();
  }, [translateY]);
  const springBackAction = useRef(springBack);
  springBackAction.current = springBack;

  const pan = useRef(
    PanResponder.create({
      // 탭은 통과시킨다 — X 버튼·시트 안 버튼이 정상 동작해야 한다.
      onStartShouldSetPanResponder: () => false,
      // 아래 방향의 세로 드래그일 때만 잡는다. 잡히는 순간 dy는 0으로 초기화되므로
      // (RN PanResponder가 grant에서 리셋한다) 문턱을 올려도 시트가 튀지 않는다.
      onMoveShouldSetPanResponder: (_, g) => g.dy > CAPTURE_DISTANCE && Math.abs(g.dy) > Math.abs(g.dx),
      // 위로 끄는 건 무시(시트가 천장에 붙어 늘어나 보이지 않게).
      onPanResponderMove: (_, g) => {
        if (g.dy > 0) {
          offsetRef.current = g.dy;
          translateY.setValue(g.dy);
        }
      },
      onPanResponderRelease: (_, g) => {
        if (shouldDismissSheet(g.dy, g.vy)) {
          closeAction.current();
          return;
        }
        springBackAction.current();
      },
      // 제스처를 뺏겼을 때도 제자리로(시트가 반쯤 내려간 채 남지 않게).
      onPanResponderTerminate: () => springBackAction.current(),
    }),
  ).current;

  return { translateY, panHandlers: pan.panHandlers, onLayout, requestClose };
}
