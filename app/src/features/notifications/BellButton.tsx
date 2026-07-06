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
    <Pressable style={[styles.btn, style]} onPress={() => navigation.navigate('Notifications')} hitSlop={10}>
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
  // 박스 없는 맨 아이콘 — 홈 검색줄 안·더보기 헤더 어디에 놓여도 종+뱃지만 보인다.
  btn: {
    width: 32,
    height: 32,
    alignItems: 'center',
    justifyContent: 'center',
  },
  badge: {
    position: 'absolute',
    top: -1,
    right: -3,
    minWidth: 16,
    height: 16,
    borderRadius: 8,
    paddingHorizontal: 4,
    backgroundColor: T2.brand,
    alignItems: 'center',
    justifyContent: 'center',
  },
  badgeText: { fontSize: 9, fontWeight: '800', color: '#fff' },
});
