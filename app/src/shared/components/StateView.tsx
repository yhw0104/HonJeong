// StateView — 로딩/빈/에러 공용 상태 표시. 화면마다 손코딩하던 것을 통일한다.
// compact=시트·리스트 안쪽(작은 패딩), 기본=화면 중앙(flex:1). error/empty에 onRetry 주면 '다시 시도' 버튼.
import React from 'react';
import { View, Text, ActivityIndicator, Pressable, StyleSheet } from 'react-native';
import { T2 } from '@/shared/theme';
import { Icon } from './Icon';
import type { IconName } from './Icon';

export type StateKind = 'loading' | 'empty' | 'error';

export function StateView({
  kind,
  message,
  icon,
  onRetry,
  compact = false,
}: {
  kind: StateKind;
  message?: string;
  icon?: IconName;
  onRetry?: () => void;
  compact?: boolean;
}) {
  const wrap = compact ? styles.compact : styles.full;
  if (kind === 'loading') {
    return (
      <View style={wrap}>
        <ActivityIndicator color={T2.brand} />
      </View>
    );
  }
  const text = message ?? (kind === 'error' ? '불러오지 못했어요' : '아직 없어요');
  return (
    <View style={wrap}>
      {icon ? <Icon name={icon} size={28} color={T2.textMute} /> : null}
      <Text style={[styles.msg, icon ? { marginTop: 12 } : null]}>{text}</Text>
      {onRetry ? (
        <Pressable style={styles.retry} onPress={onRetry} hitSlop={8} accessibilityRole="button">
          <Text style={styles.retryText}>다시 시도</Text>
        </Pressable>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  full: { flex: 1, alignItems: 'center', justifyContent: 'center', paddingHorizontal: 32, paddingVertical: 48 },
  compact: { alignItems: 'center', justifyContent: 'center', paddingHorizontal: 24, paddingVertical: 28 },
  msg: { fontSize: 14, color: T2.textSub, textAlign: 'center', lineHeight: 21 },
  retry: {
    marginTop: 14,
    paddingHorizontal: 18,
    paddingVertical: 9,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: T2.brand,
  },
  retryText: { fontSize: 13, fontWeight: '700', color: T2.brand, letterSpacing: -0.2 },
});
