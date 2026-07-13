// SlideToConfirm — '밀어서 완료' 슬라이드 확인 컨트롤. 실수 탭 방지용(일부러 끝까지 밀어야 onConfirm).
// 썸(둥근 사각 손잡이)을 오른쪽 끝 75%까지 밀면 확정, 그 전에 놓으면 처음으로 스프링백.
import React, { useRef } from 'react';
import { View, Text, Animated, PanResponder, StyleSheet, type StyleProp, type ViewStyle } from 'react-native';
import { T2 } from '@/shared/theme';

const TRACK_H = 58;
const THUMB = 50;
const PAD = 4;

export function SlideToConfirm({ label, onConfirm, style }: {
  label: string;
  onConfirm: () => void;
  style?: StyleProp<ViewStyle>;
}) {
  const x = useRef(new Animated.Value(0)).current;
  const maxXRef = useRef(0); // 트랙 폭에 따른 최대 이동거리(레이아웃에서 갱신 — 팬핸들러 stale 방지)
  const confirmedRef = useRef(false);

  const pan = useRef(
    PanResponder.create({
      onStartShouldSetPanResponder: () => true,
      onMoveShouldSetPanResponder: () => true,
      onPanResponderMove: (_, g) => {
        if (confirmedRef.current) return;
        x.setValue(Math.min(Math.max(0, g.dx), maxXRef.current));
      },
      onPanResponderRelease: (_, g) => {
        const max = maxXRef.current;
        const nx = Math.min(Math.max(0, g.dx), max);
        if (max > 0 && nx >= max * 0.75) {
          confirmedRef.current = true;
          Animated.timing(x, { toValue: max, duration: 110, useNativeDriver: false }).start(() => onConfirm());
        } else {
          Animated.spring(x, { toValue: 0, useNativeDriver: false, bounciness: 6 }).start();
        }
      },
    }),
  ).current;

  return (
    <View
      style={[styles.track, style]}
      onLayout={(e) => { maxXRef.current = Math.max(0, e.nativeEvent.layout.width - THUMB - PAD * 2); }}
    >
      <Text style={styles.label} numberOfLines={1}>{label}</Text>
      <Animated.View style={[styles.thumb, { transform: [{ translateX: x }] }]} {...pan.panHandlers}>
        <Text style={styles.arrow}>›</Text>
      </Animated.View>
    </View>
  );
}

const styles = StyleSheet.create({
  track: { height: TRACK_H, borderRadius: 16, backgroundColor: T2.brandSoft, justifyContent: 'center', overflow: 'hidden' },
  label: { textAlign: 'center', color: T2.brand, fontWeight: '800', fontSize: 15, letterSpacing: -0.3 },
  thumb: {
    position: 'absolute', left: PAD, top: PAD, width: THUMB, height: THUMB, borderRadius: 12,
    backgroundColor: T2.brand, alignItems: 'center', justifyContent: 'center',
    shadowColor: T2.brand, shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.35, shadowRadius: 6, elevation: 4,
  },
  arrow: { color: '#fff', fontSize: 26, fontWeight: '900', marginTop: -3, marginLeft: 2 },
});
