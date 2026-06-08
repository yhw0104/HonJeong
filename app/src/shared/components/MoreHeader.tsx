// MoreHeader — 뒤로가기 + 타이틀 헤더 (더보기 하위 화면들 공통).
// 목업 사용: <MoreHeader title="메이트" />
import React from 'react';
import { View, Text, Pressable, StyleSheet } from 'react-native';
import { T2 } from '@/shared/theme';

type Props = {
  title: string;
  onBack?: () => void;
  right?: React.ReactNode;
};

export function MoreHeader({ title, onBack, right }: Props) {
  return (
    <View style={styles.row}>
      <Pressable onPress={onBack} hitSlop={10} style={styles.back}>
        <Text style={styles.arrow}>←</Text>
      </Pressable>
      <Text style={styles.title}>{title}</Text>
      <View style={styles.right}>{right}</View>
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    height: 56,
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
  },
  back: { width: 32, height: 32, alignItems: 'flex-start', justifyContent: 'center' },
  arrow: { fontSize: 22, color: T2.text },
  title: { flex: 1, fontSize: 17, fontWeight: '800', color: T2.text, letterSpacing: -0.4 },
  right: { minWidth: 32, alignItems: 'flex-end' },
});
