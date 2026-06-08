// Toggle — 목업의 커스텀 스위치(공개설정 등). controlled.
import React from 'react';
import { Pressable, View, StyleSheet } from 'react-native';
import { T2 } from '@/shared/theme';

type Props = {
  value: boolean;
  onValueChange?: (next: boolean) => void;
};

export function Toggle({ value, onValueChange }: Props) {
  return (
    <Pressable
      onPress={() => onValueChange?.(!value)}
      style={[styles.track, { backgroundColor: value ? T2.brand : T2.borderStrong }]}
    >
      <View style={[styles.knob, value ? styles.knobOn : styles.knobOff]} />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  track: { width: 46, height: 28, borderRadius: 14, justifyContent: 'center' },
  knob: {
    position: 'absolute',
    width: 22,
    height: 22,
    borderRadius: 11,
    backgroundColor: '#fff',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.2,
    shadowRadius: 3,
    elevation: 2,
  },
  knobOn: { right: 3 },
  knobOff: { left: 3 },
});
