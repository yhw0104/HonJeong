// BellButton — 종 아이콘 + 안읽음 뱃지. 홈 검색줄·더보기 헤더 공용. 탭하면 알림함으로.
import React from 'react';
import { Pressable, View, Text, StyleSheet } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { Icon } from '@/shared/components';
import { T2 } from '@/shared/theme';
import { useUnreadCount } from '@/features/notifications/queries';
import type { RootStackParamList } from '@/navigation/types';

export function BellButton({ style }: { style?: object }) {
  const navigation = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const { data } = useUnreadCount();
  const count = data?.count ?? 0;

  return (
    <Pressable style={[styles.btn, style]} onPress={() => navigation.navigate('Notifications')} hitSlop={6}>
      <Icon name="bell" size={20} color={T2.text} />
      {count > 0 ? (
        <View style={styles.badge}>
          <Text style={styles.badgeText}>{count > 99 ? '99+' : String(count)}</Text>
        </View>
      ) : null}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  // MapHome navBtn과 동일한 48×48 라운드 사각형 — 검색줄에 나란히 놓여도 높이가 어긋나지 않도록.
  btn: {
    width: 48,
    height: 48,
    borderRadius: 14,
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: T2.border,
    alignItems: 'center',
    justifyContent: 'center',
  },
  badge: {
    position: 'absolute',
    top: -2,
    right: -2,
    minWidth: 18,
    height: 18,
    borderRadius: 9,
    paddingHorizontal: 5,
    backgroundColor: T2.brand,
    alignItems: 'center',
    justifyContent: 'center',
  },
  badgeText: { fontSize: 10, fontWeight: '800', color: '#fff' },
});
