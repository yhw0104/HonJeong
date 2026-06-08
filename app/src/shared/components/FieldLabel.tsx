// FieldLabel — 폼 섹션의 작은 뮤트 라벨.
// 목업 사용: <FieldLabel>닉네임</FieldLabel>
import React from 'react';
import { Text, TextStyle } from 'react-native';
import { T2 } from '@/shared/theme';

export function FieldLabel({ children, style }: { children: React.ReactNode; style?: TextStyle }) {
  return (
    <Text
      style={[
        { fontSize: 12, fontWeight: '700', color: T2.textMute, letterSpacing: 0.5, marginBottom: 10 },
        style,
      ]}
    >
      {children}
    </Text>
  );
}
