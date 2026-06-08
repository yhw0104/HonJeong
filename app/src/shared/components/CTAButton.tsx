// CTAButton — 하단 고정 주요 액션 버튼 (인증/온보딩 화면 공통).
import React from 'react';
import { Pressable, Text, StyleSheet, ViewStyle } from 'react-native';
import { T2 } from '@/shared/theme';

type Props = {
  label: string;
  onPress?: () => void;
  color?: string;
  textColor?: string;
  disabled?: boolean;
  style?: ViewStyle;
};

export function CTAButton({ label, onPress, color = T2.brand, textColor = '#fff', disabled, style }: Props) {
  return (
    <Pressable
      onPress={onPress}
      disabled={disabled}
      style={({ pressed }) => [
        styles.btn,
        { backgroundColor: color, opacity: disabled ? 0.45 : pressed ? 0.9 : 1 },
        style,
      ]}
    >
      <Text style={[styles.label, { color: textColor }]}>{label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  btn: { paddingVertical: 16, borderRadius: 12, alignItems: 'center', justifyContent: 'center' },
  label: { fontSize: 15, fontWeight: '700', letterSpacing: -0.3 },
});
