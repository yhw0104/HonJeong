// PhotoViewer — 사진 한 장을 전체화면으로 크게 보는 뷰어(프로필 사진·대화 속 사진 공용).
//
// 조작:
//   · 핀치        확대/축소(최대 5배). 원배율보다 작게 밀면 손을 뗄 때 원배율로 되돌아온다.
//   · 더블탭      원배율 ↔ 2.5배 토글(한 손으로 빠르게 확대할 때).
//   · 드래그      **확대 중**이면 사진 이동(가장자리를 넘지 않게 잘린다).
//                 **원배율**이면 아래로 끌어 닫기 — 따라 내려오고 배경이 함께 옅어진다.
//   · 탭          **아무 일도 하지 않는다.** 사진을 짚는 건 닫으려는 게 아니라 보려는 동작이다.
//   · ×           확대 중일 때의 출구(안드로이드는 뒤로가기도).
//
// 제스처는 Modal 안에 있으므로 GestureHandlerRootView로 한 번 더 감싼다 —
// App.tsx의 루트는 Modal 내부까지 미치지 않아, 없으면 안드로이드에서 제스처가 죽는다.
//
// uri가 null이면 렌더하지 않는다(= 닫힌 상태). 호출부는 상태 하나만 들고 있으면 된다.
import React, { useCallback, useEffect } from 'react';
import { Modal, View, Text, Image, Pressable, StyleSheet, useWindowDimensions } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Gesture, GestureDetector, GestureHandlerRootView } from 'react-native-gesture-handler';
import Animated, { useSharedValue, useAnimatedStyle, withTiming, runOnJS } from 'react-native-reanimated';
import { shouldDismissSheet } from './useSheetDismissGesture';

const MAX_SCALE = 5;
const DOUBLE_TAP_SCALE = 2.5;
// 핀치를 원배율 밑으로 밀 때 이만큼까지는 따라가게 두고(고무줄 느낌), 손을 떼면 1로 되돌린다.
const MIN_PINCH_SCALE = 0.7;
// 끌어 내린 만큼 배경이 옅어진다 — 이 거리에서 완전히 투명해진다(닫히는 중임을 손에 알려준다).
const DISMISS_FADE_DISTANCE = 320;
// 닫기로 판정되면 화면 밖까지 마저 미끄러진다. 시트와 같은 감각으로.
const DISMISS_OUT_MS = 200;

function clamp(v: number, min: number, max: number) {
  'worklet';
  return Math.min(max, Math.max(min, v));
}

export function PhotoViewer({ uri, onClose }: { uri: string | null; onClose: () => void }) {
  const { width, height } = useWindowDimensions();
  const insets = useSafeAreaInsets();

  const scale = useSharedValue(1);
  const savedScale = useSharedValue(1);
  const tx = useSharedValue(0);
  const ty = useSharedValue(0);
  const savedTx = useSharedValue(0);
  const savedTy = useSharedValue(0);

  /** 배경 불투명도(1=완전히 검정). 끌어 내리는 동안 함께 옅어진다. */
  const backdrop = useSharedValue(1);

  // ★초기화는 **열릴 때** 한다(닫을 때가 아니라). 뷰어를 재사용하므로 되돌리지 않으면 다음에
  // 확대된 채로 뜨는데, 그렇다고 닫는 순간에 되돌리면 사라지는 중에 사진이 제자리로 튀는 게
  // 보인다 — 시트에서 겪은 것과 같은 문제라 같은 방식으로 푼다.
  useEffect(() => {
    if (uri == null) return;
    scale.value = 1;
    savedScale.value = 1;
    tx.value = 0;
    ty.value = 0;
    savedTx.value = 0;
    savedTy.value = 0;
    backdrop.value = 1;
  }, [uri, scale, savedScale, tx, ty, savedTx, savedTy, backdrop]);

  const close = useCallback(() => {
    onClose();
  }, [onClose]);

  const pinch = Gesture.Pinch()
    .onUpdate((e) => {
      scale.value = clamp(savedScale.value * e.scale, MIN_PINCH_SCALE, MAX_SCALE);
    })
    .onEnd(() => {
      if (scale.value <= 1) {
        scale.value = withTiming(1);
        tx.value = withTiming(0);
        ty.value = withTiming(0);
        savedScale.value = 1;
        savedTx.value = 0;
        savedTy.value = 0;
        return;
      }
      savedScale.value = scale.value;
      // 배율이 줄면 허용 이동 범위도 줄어든다 — 범위를 넘긴 위치는 안으로 당겨 넣는다.
      const maxX = (width * (scale.value - 1)) / 2;
      const maxY = (height * (scale.value - 1)) / 2;
      const nx = clamp(tx.value, -maxX, maxX);
      const ny = clamp(ty.value, -maxY, maxY);
      tx.value = withTiming(nx);
      ty.value = withTiming(ny);
      savedTx.value = nx;
      savedTy.value = ny;
    });

  // 드래그는 배율에 따라 뜻이 완전히 갈린다.
  //
  // ★확대 중에는 **절대로 닫지 않는다.** 확대해서 보는 중의 아래 드래그는 "사진의 아래쪽을
  //   보고 싶다"는 뜻이지 "닫아 달라"가 아니다. 여기서 닫아버리면 확대해서 볼 방법이 없어진다.
  //   그래서 판정 기준을 손짓의 모양이 아니라 **현재 배율**로 둔다 — 모호할 여지가 없다.
  const pan = Gesture.Pan()
    .averageTouches(true)
    .onUpdate((e) => {
      if (scale.value > 1) {
        // 끌어 내리다가 핀치로 확대하면 여기로 넘어온다 — 옅어진 배경을 되돌리지 않으면
        // 확대해서 보는 내내 배경이 반쯤 투명한 채로 남는다.
        backdrop.value = 1;
        const maxX = (width * (scale.value - 1)) / 2;
        const maxY = (height * (scale.value - 1)) / 2;
        tx.value = clamp(savedTx.value + e.translationX, -maxX, maxX);
        ty.value = clamp(savedTy.value + e.translationY, -maxY, maxY);
        return;
      }
      // 원배율 — 아래로 끌어 닫기. 위로 끄는 건 무시한다(사진이 천장 위로 뜨지 않게).
      if (e.translationY <= 0) {
        ty.value = 0;
        backdrop.value = 1;
        return;
      }
      ty.value = e.translationY;
      backdrop.value = clamp(1 - e.translationY / DISMISS_FADE_DISTANCE, 0, 1);
    })
    .onEnd((e) => {
      if (scale.value > 1) {
        savedTx.value = tx.value;
        savedTy.value = ty.value;
        return;
      }
      // velocityY는 px/초, shouldDismissSheet는 px/ms를 받는다.
      if (shouldDismissSheet(e.translationY, e.velocityY / 1000)) {
        backdrop.value = withTiming(0, { duration: DISMISS_OUT_MS });
        ty.value = withTiming(height, { duration: DISMISS_OUT_MS }, (finished) => {
          if (finished) runOnJS(close)();
        });
        return;
      }
      ty.value = withTiming(0);
      backdrop.value = withTiming(1);
    });

  const doubleTap = Gesture.Tap()
    .numberOfTaps(2)
    .onEnd(() => {
      if (scale.value > 1) {
        scale.value = withTiming(1);
        tx.value = withTiming(0);
        ty.value = withTiming(0);
        savedScale.value = 1;
        savedTx.value = 0;
        savedTy.value = 0;
      } else {
        scale.value = withTiming(DOUBLE_TAP_SCALE);
        savedScale.value = DOUBLE_TAP_SCALE;
      }
    });

  // ★사진을 탭해도 닫지 않는다. 크게 보는 중에 사진을 짚는 것은 "닫아 달라"가 아니라
  // 이리저리 보려는 동작이고, 손이 스치기만 해도 닫히면 다시 열어야 한다.
  // 출구는 ×와 (안드로이드) 뒤로가기뿐이다.
  //
  // 08-04에는 '확대 중일 때만' 탭 닫기를 막았는데, 원배율에서도 같은 불편이 남는다는
  // 실기 피드백으로 아예 없앴다. 더블탭 확대는 그대로 둔다.
  const gesture = Gesture.Simultaneous(pinch, pan, doubleTap);

  const animStyle = useAnimatedStyle(() => ({
    transform: [{ translateX: tx.value }, { translateY: ty.value }, { scale: scale.value }],
  }));
  // 배경과 ×만 옅어진다 — 사진 자체는 또렷한 채로 내려가야 "들고 내리는" 느낌이 난다.
  const backdropStyle = useAnimatedStyle(() => ({ opacity: backdrop.value }));

  return (
    <Modal
      visible={uri != null}
      transparent
      animationType="fade"
      onRequestClose={close} // 안드로이드 뒤로가기
      statusBarTranslucent
    >
      <GestureHandlerRootView style={styles.root}>
        {/* 검은 배경은 따로 깐다 — 사진과 같은 뷰에 있으면 끌어 내릴 때 사진까지 같이 옅어진다. */}
        <Animated.View style={[styles.backdrop, backdropStyle]} pointerEvents="none" />
        <View style={styles.center}>
          <GestureDetector gesture={gesture}>
            <Animated.View style={[{ width, height }, animStyle]}>
              {uri ? (
                // contain — 세로로 긴 사진도 잘리지 않게. 확대는 이 컨테이너째로 한다.
                <Image source={{ uri }} style={{ width, height }} resizeMode="contain" />
              ) : null}
            </Animated.View>
          </GestureDetector>
        </View>
        {/* 확대 상태에서는 아래로 끌어도 닫히지 않으므로(그건 사진 이동이다) ×가 그때의 출구다. */}
        <Pressable
          style={[styles.close, { top: insets.top + 8 }]}
          onPress={close}
          hitSlop={12}
          accessibilityRole="button"
          accessibilityLabel="닫기"
        >
          <Animated.View style={[styles.closeBg, backdropStyle]}>
            <Text style={styles.closeX}>×</Text>
          </Animated.View>
        </Pressable>
      </GestureHandlerRootView>
    </Modal>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1 },
  backdrop: { position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0,0,0,0.92)' },
  // overflow:hidden — 확대한 사진이 화면 밖으로 삐져나와 그려지지 않게.
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', overflow: 'hidden' },
  close: { position: 'absolute', right: 14 },
  closeBg: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: 'rgba(255,255,255,0.16)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  closeX: { fontSize: 22, color: '#fff', lineHeight: 24 },
});
