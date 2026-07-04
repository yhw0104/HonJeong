// Notifications — 인앱 알림함 (NOTI-003). 홈 종 버튼·더보기 우상단에서 진입.
// 탭하면 읽음 처리 + 관련 화면 이동. 문구는 copy.ts 순수 함수로 조립.
import React from 'react';
import { View, Text, Pressable, ScrollView, StyleSheet, ActivityIndicator } from 'react-native';
import { Screen, MoreHeader } from '@/shared/components';
import { T2 } from '@/shared/theme';
import { formatTimeAgo } from '@/shared/format';
import { useNotifications, useMarkRead, useMarkAllRead } from '@/features/notifications/queries';
import { notificationMessage, notificationTarget, notificationEmoji } from '@/features/notifications/copy';
import type { NotificationItem } from '@/features/notifications/api';
import type { RootStackScreenProps } from '@/navigation/types';

export function NotificationsScreen({ navigation }: RootStackScreenProps<'Notifications'>) {
  const list = useNotifications();
  const markRead = useMarkRead();
  const markAll = useMarkAllRead();
  const items = list.data ?? [];
  const hasUnread = items.some((n) => !n.isRead);

  const onPress = (n: NotificationItem) => {
    if (!n.isRead) markRead.mutate(n.id);
    const target = notificationTarget(n.type);
    // 'MainTabs'는 탭 네비게이터의 기존 탭 상태를 보존하므로, 홈 탭(MapHome)을 명시해
    // 더보기 탭 등 다른 탭에서 진입했더라도 스펙대로 홈 지도로 이동시킨다.
    if (target === 'MainTabs') {
      navigation.navigate('MainTabs', { screen: 'MapHome' });
      return;
    }
    navigation.navigate(target);
  };

  return (
    <Screen bg={T2.bg} edges={['top']}>
      <MoreHeader
        title="알림"
        onBack={() => navigation.goBack()}
        right={
          hasUnread ? (
            <Pressable onPress={() => markAll.mutate()} hitSlop={8} disabled={markAll.isPending}>
              <Text style={styles.readAll}>모두 읽음</Text>
            </Pressable>
          ) : undefined
        }
      />
      {list.isLoading ? (
        <ActivityIndicator style={{ marginTop: 40 }} color={T2.brand} />
      ) : items.length === 0 ? (
        <View style={styles.empty}>
          <Text style={styles.emptyEmoji}>🔔</Text>
          <Text style={styles.emptyTitle}>아직 알림이 없어요</Text>
          <Text style={styles.emptySub}>같이 먹기·메이트 소식이 여기에 쌓여요</Text>
        </View>
      ) : (
        <ScrollView contentContainerStyle={{ paddingBottom: 24 }}>
          {items.map((n) => (
            <Pressable
              key={n.id}
              style={[styles.row, !n.isRead && styles.rowUnread]}
              onPress={() => onPress(n)}
            >
              <View style={styles.iconCircle}>
                <Text style={{ fontSize: 18 }}>{notificationEmoji(n.type)}</Text>
              </View>
              <View style={{ flex: 1, minWidth: 0 }}>
                <Text style={[styles.message, !n.isRead && styles.messageUnread]}>
                  {notificationMessage(n.type, n.actorNickname)}
                </Text>
                <Text style={styles.time}>{formatTimeAgo(n.createdAt, new Date())}</Text>
              </View>
              {!n.isRead ? <View style={styles.unreadDot} /> : null}
            </Pressable>
          ))}
        </ScrollView>
      )}
    </Screen>
  );
}

const styles = StyleSheet.create({
  readAll: { fontSize: 13, fontWeight: '700', color: T2.brand },
  empty: { alignItems: 'center', marginTop: 80, gap: 6 },
  emptyEmoji: { fontSize: 40 },
  emptyTitle: { fontSize: 16, fontWeight: '700', color: T2.text, marginTop: 8 },
  emptySub: { fontSize: 13, color: T2.textMute },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    paddingVertical: 14,
    paddingHorizontal: 20,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: T2.border,
  },
  rowUnread: { backgroundColor: T2.brandSoft },
  iconCircle: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: T2.bg,
    alignItems: 'center',
    justifyContent: 'center',
  },
  message: { fontSize: 14, color: T2.textSub, letterSpacing: -0.3, lineHeight: 20 },
  messageUnread: { color: T2.text, fontWeight: '600' },
  time: { fontSize: 11, color: T2.textMute, marginTop: 2 },
  unreadDot: { width: 8, height: 8, borderRadius: 4, backgroundColor: T2.brand },
});
