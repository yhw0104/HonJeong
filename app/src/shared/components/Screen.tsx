// Screen — 목업의 <PhoneShell> 래퍼를 대체.
// 폰 프레임 chrome은 제거하고, 실기기 SafeArea + 배경색만 제공한다.
import React from 'react';
import { StyleSheet, ViewStyle } from 'react-native';
import { SafeAreaView, Edge } from 'react-native-safe-area-context';
import { T2 } from '@/shared/theme';

type Props = {
  children: React.ReactNode;
  bg?: string;
  edges?: readonly Edge[];
  style?: ViewStyle;
};

export function Screen({ children, bg = T2.bg, edges, style }: Props) {
  return (
    <SafeAreaView style={[styles.root, { backgroundColor: bg }, style]} edges={edges}>
      {children}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1 },
});
