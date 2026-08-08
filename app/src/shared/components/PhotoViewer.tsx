// PhotoViewer — 사진 한 장을 전체화면으로 크게 보는 뷰어(프로필 사진·대화 속 사진 공용).
//
// 조작:
//   · 핀치        확대/축소(최대 5배). 원배율보다 작게 밀면 손을 뗄 때 원배율로 되돌아온다.
//   · 더블탭      원배율 ↔ 2.5배 토글(한 손으로 빠르게 확대할 때).
//   · 드래그      확대 상태에서만 이동. 가장자리를 넘어가지 않게 잘린다.
//   · 탭          **아무 일도 하지 않는다.** 사진을 짚는 건 닫으려는 게 아니라 보려는 동작이다.
//   · ×           유일한 출구(안드로이드는 뒤로가기도).
//
// 제스처는 Modal 안에 있으므로 GestureHandlerRootView로 한 번 더 감싼다 —
// App.tsx의 루트는 Modal 내부까지 미치지 않아, 없으면 안드로이드에서 제스처가 죽는다.
//
// uri가 null이면 렌더하지 않는다(= 닫힌 상태). 호출부는 상태 하나만 들고 있으면 된다.
import React, { useCallback } from 'react';
import { Modal, View, Text, Image, Pressable, StyleSheet, useWindowDimensions } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Gesture, GestureDetector, GestureHandlerRootView } from 'react-native-gesture-handler';
import Animated, { useSharedValue, useAnimatedStyle, withTiming } from 'react-native-reanimated';

const MAX_SCALE = 5;
const DOUBLE_TAP_SCALE = 2.5;
// 핀치를 원배율 밑으로 밀 때 이만큼까지는 따라가게 두고(고무줄 느낌), 손을 떼면 1로 되돌린다.
const MIN_PINCH_SCALE = 0.7;

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

  // 닫을 때 배율·위치를 초기화한다 — 안 하면 다음에 열 때 확대된 채로 뜬다(뷰어를 재사용하므로).
  const close = useCallback(() => {
    scale.value = 1;
    savedScale.value = 1;
    tx.value = 0;
    ty.value = 0;
    savedTx.value = 0;
    savedTy.value = 0;
    onClose();
  }, [onClose, scale, savedScale, tx, ty, savedTx, savedTy]);

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

  const pan = Gesture.Pan()
    .averageTouches(true)
    .onUpdate((e) => {
      if (scale.value <= 1) return; // 원배율에서는 움직이지 않는다(탭으로 닫는 동작과 섞이지 않게)
      const maxX = (width * (scale.value - 1)) / 2;
      const maxY = (height * (scale.value - 1)) / 2;
      tx.value = clamp(savedTx.value + e.translationX, -maxX, maxX);
      ty.value = clamp(savedTy.value + e.translationY, -maxY, maxY);
    })
    .onEnd(() => {
      savedTx.value = tx.value;
      savedTy.value = ty.value;
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

  return (
    <Modal
      visible={uri != null}
      transparent
      animationType="fade"
      onRequestClose={close} // 안드로이드 뒤로가기
      statusBarTranslucent
    >
      <GestureHandlerRootView style={styles.root}>
        <View style={styles.backdrop}>
          <GestureDetector gesture={gesture}>
            <Animated.View style={[{ width, height }, animStyle]}>
              {uri ? (
                // contain — 세로로 긴 사진도 잘리지 않게. 확대는 이 컨테이너째로 한다.
                <Image source={{ uri }} style={{ width, height }} resizeMode="contain" />
              ) : null}
            </Animated.View>
          </GestureDetector>
        </View>
        {/* 확대 상태에서는 탭으로 안 닫히므로 ×가 유일한 출구다 — 제스처 영역 밖에 둔다. */}
        <Pressable
          style={[styles.close, { top: insets.top + 8 }]}
          onPress={close}
          hitSlop={12}
          accessibilityRole="button"
          accessibilityLabel="닫기"
        >
          <View style={styles.closeBg}>
            <Text style={styles.closeX}>×</Text>
          </View>
        </Pressable>
      </GestureHandlerRootView>
    </Modal>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1 },
  // overflow:hidden — 확대한 사진이 배경 밖으로 삐져나와 그려지지 않게.
  backdrop: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.92)',
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
  },
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
