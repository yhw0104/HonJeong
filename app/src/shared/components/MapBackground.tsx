// MapBackground — 실제 지도(react-native-maps) 도입 전까지 쓰는 플레이스홀더.
// 베이지 배경 위에 도로/블록을 단순 도형으로 흉내낸다.
import React from 'react';
import { View, StyleSheet } from 'react-native';
import { T2 } from '@/shared/theme';

export function MapBackground() {
  return (
    <View style={[StyleSheet.absoluteFill, { backgroundColor: T2.mapBg }]}>
      {/* 블록(건물 구역) */}
      {BLOCKS.map((b, i) => (
        <View
          key={`b${i}`}
          style={{
            position: 'absolute',
            left: b.x,
            top: b.y,
            width: b.w,
            height: b.h,
            borderRadius: 6,
            backgroundColor: '#F3EFE6',
          }}
        />
      ))}
      {/* 가로 도로 */}
      {[140, 300, 470, 640].map((top, i) => (
        <View key={`h${i}`} style={[styles.roadH, { top }]} />
      ))}
      {/* 세로 도로 */}
      {[80, 200, 320].map((left, i) => (
        <View key={`v${i}`} style={[styles.roadV, { left }]} />
      ))}
    </View>
  );
}

const BLOCKS = [
  { x: 24, y: 60, w: 110, h: 64 },
  { x: 220, y: 90, w: 120, h: 90 },
  { x: 40, y: 230, w: 90, h: 100 },
  { x: 230, y: 340, w: 110, h: 80 },
  { x: 60, y: 500, w: 130, h: 90 },
  { x: 250, y: 540, w: 100, h: 110 },
];

const styles = StyleSheet.create({
  roadH: { position: 'absolute', left: 0, right: 0, height: 10, backgroundColor: '#F7F4EC' },
  roadV: { position: 'absolute', top: 0, bottom: 0, width: 10, backgroundColor: '#F7F4EC' },
});
