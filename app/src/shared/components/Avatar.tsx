// Avatar — 이름 첫 글자(또는 이모지)를 원형 배경에 표시.
// 목업 사용: <Avatar name="혼" bg={T2.text} size={84} ring="#FFF4EF" />
import React from 'react';
import { View, Text } from 'react-native';
import { T2 } from '@/shared/theme';

type Props = {
  name: string;
  bg?: string;
  size?: number;
  ring?: string;
  color?: string;
};

export function Avatar({ name, bg = T2.text, size = 40, ring, color = '#fff' }: Props) {
  const initial = Array.from(name ?? '')[0] ?? '';
  return (
    <View
      style={{
        width: size,
        height: size,
        borderRadius: size / 2,
        backgroundColor: bg,
        alignItems: 'center',
        justifyContent: 'center',
        borderWidth: ring ? 2.5 : 0,
        borderColor: ring,
      }}
    >
      <Text style={{ color, fontWeight: '800', fontSize: Math.round(size * 0.42) }}>{initial}</Text>
    </View>
  );
}
