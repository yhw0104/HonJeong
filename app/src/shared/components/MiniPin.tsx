// MiniPin — 지도 위 식당/혼밥러 핀. 지도 컨테이너 내부에 절대 위치로 배치.
// 목업 사용: <MiniPin x={120} y={320} label="큰순두부" mate />
import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { T2 } from '@/shared/theme';

type Props = {
  x: number;
  y: number;
  label?: string;
  active?: boolean;
  mate?: boolean;
};

export function MiniPin({ x, y, label, active, mate }: Props) {
  const dotBg = mate ? T2.brand : '#fff';
  const dotBorder = mate ? T2.brand : active ? T2.text : T2.borderStrong;
  const innerBg = mate ? '#fff' : active ? T2.text : T2.brand;
  return (
    <View style={[styles.wrap, { left: x, top: y }]}>
      {label ? (
        <View style={styles.bubble}>
          <Text style={styles.bubbleText} numberOfLines={1}>
            {label}
          </Text>
        </View>
      ) : null}
      <View style={[styles.dot, { backgroundColor: dotBg, borderColor: dotBorder }]}>
        <View style={[styles.inner, { backgroundColor: innerBg }]} />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  wrap: {
    position: 'absolute',
    alignItems: 'center',
    transform: [{ translateX: -12 }, { translateY: -12 }],
  },
  bubble: {
    marginBottom: 4,
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 8,
    backgroundColor: '#fff',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.12,
    shadowRadius: 6,
    elevation: 3,
    maxWidth: 160,
  },
  bubbleText: { fontSize: 11, fontWeight: '700', color: T2.text, letterSpacing: -0.2 },
  dot: {
    width: 24,
    height: 24,
    borderRadius: 12,
    borderWidth: 2,
    alignItems: 'center',
    justifyContent: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.12,
    shadowRadius: 4,
    elevation: 2,
  },
  inner: { width: 8, height: 8, borderRadius: 4 },
});
