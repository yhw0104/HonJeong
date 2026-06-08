// StepProgress — 인증/온보딩의 "01 ──── 03" 단계 진행 표시.
import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { T2 } from '@/shared/theme';

const pad = (n: number) => String(n).padStart(2, '0');

export function StepProgress({ step, total = 3 }: { step: number; total?: number }) {
  const ratio = Math.max(0, Math.min(1, step / total));
  const done = step >= total;
  return (
    <View style={styles.row}>
      <Text style={styles.numActive}>{pad(step)}</Text>
      <View style={styles.track}>
        <View style={[styles.fill, { width: `${ratio * 100}%` }]} />
      </View>
      <Text style={done ? styles.numActive : styles.numMute}>{pad(total)}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center', gap: 8, marginBottom: 28 },
  track: { flex: 1, height: 2, backgroundColor: T2.border },
  fill: { position: 'absolute', left: 0, top: 0, height: '100%', backgroundColor: T2.brand },
  numActive: { fontSize: 12, fontWeight: '800', color: T2.text },
  numMute: { fontSize: 12, fontWeight: '600', color: T2.textMute },
});
