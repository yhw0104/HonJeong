// EmojiCircle — 이모지를 담는 원형 아바타(+ 선택적 '혼밥 중' 초록 점).
// 메이트/신청/프로필 화면에서 반복되는 패턴을 추출.
import React from 'react';
import { View, Text } from 'react-native';
import { T2, C } from '@/shared/theme';

type Props = {
  emoji: string;
  size?: number;
  online?: boolean;
  dimmed?: boolean;
};

export function EmojiCircle({ emoji, size = 48, online = false, dimmed = false }: Props) {
  const dot = Math.round(size * 0.31);
  return (
    <View
      style={{
        width: size,
        height: size,
        borderRadius: size / 2,
        backgroundColor: T2.bg,
        borderWidth: 1,
        borderColor: T2.border,
        alignItems: 'center',
        justifyContent: 'center',
        opacity: dimmed ? 0.7 : 1,
      }}
    >
      <Text style={{ fontSize: Math.round(size * 0.46) }}>{emoji}</Text>
      {online ? (
        <View
          style={{
            position: 'absolute',
            right: -1,
            bottom: -1,
            width: dot,
            height: dot,
            borderRadius: dot / 2,
            backgroundColor: C.open,
            borderWidth: 2.5,
            borderColor: '#fff',
          }}
        />
      ) : null}
    </View>
  );
}
