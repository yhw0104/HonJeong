// ConversationList — '대화' 탭. 매칭 성사 후 열리는 대화방 목록.
import React from 'react';
import { View, Text, Pressable, FlatList, StyleSheet } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { Screen, Avatar, StateView } from '@/shared/components';
import { T2 } from '@/shared/theme';
import type { RootStackParamList } from '@/navigation/types';
import { useConversations } from '../queries';
import { messagePreview } from '../chatFormat';
import type { ConversationSummary } from '../types';

export function ConversationListScreen() {
  const nav = useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const { data, isLoading, isError, refetch } = useConversations();

  if (isLoading) {
    return (
      <Screen>
        <Text style={styles.header}>대화</Text>
        <StateView kind="loading" />
      </Screen>
    );
  }
  if (isError) {
    return (
      <Screen>
        <Text style={styles.header}>대화</Text>
        <StateView kind="error" onRetry={() => refetch()} />
      </Screen>
    );
  }

  const list = data ?? [];
  if (list.length === 0) {
    return (
      <Screen>
        <Text style={styles.header}>대화</Text>
        <StateView kind="empty" message={'아직 대화가 없어요\n같이먹기가 성사되면 여기서 대화할 수 있어요'} />
      </Screen>
    );
  }

  const renderItem = ({ item }: { item: ConversationSummary }) => (
    <Pressable style={styles.row} onPress={() => nav.navigate('ChatRoom', { conversationId: item.conversationId })}>
      <Avatar name={item.partnerNickname} uri={item.partnerProfileImageUrl} size={48} />
      <View style={styles.body}>
        <View style={styles.line}>
          <Text style={styles.name} numberOfLines={1}>{item.partnerNickname}</Text>
          {item.status === 'CLOSED' && <Text style={styles.closed}>종료됨</Text>}
        </View>
        <Text style={styles.place} numberOfLines={1}>{item.placeName}</Text>
        <Text style={styles.preview} numberOfLines={1}>
          {item.lastMessagePreview ? messagePreview({ type: 'TEXT', text: item.lastMessagePreview }) : ''}
        </Text>
      </View>
      {item.unreadCount > 0 && (
        <View style={styles.badge}><Text style={styles.badgeText}>{item.unreadCount}</Text></View>
      )}
    </Pressable>
  );

  return (
    <Screen>
      <Text style={styles.header}>대화</Text>
      <FlatList data={list} keyExtractor={(c) => String(c.conversationId)} renderItem={renderItem} />
    </Screen>
  );
}

const styles = StyleSheet.create({
  header: { fontSize: 20, fontWeight: '800', color: T2.text, paddingHorizontal: 16, paddingVertical: 12 },
  row: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingHorizontal: 16, paddingVertical: 12 },
  body: { flex: 1, gap: 2 },
  line: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  name: { fontSize: 15, fontWeight: '700', color: T2.text, flexShrink: 1 },
  closed: { fontSize: 11, fontWeight: '700', color: T2.textMute, backgroundColor: T2.bg, paddingHorizontal: 6, paddingVertical: 1, borderRadius: 6 },
  place: { fontSize: 12, color: T2.textSub },
  preview: { fontSize: 13, color: T2.textSub },
  badge: { minWidth: 20, height: 20, borderRadius: 10, backgroundColor: T2.brand, alignItems: 'center', justifyContent: 'center', paddingHorizontal: 6 },
  badgeText: { color: '#fff', fontSize: 11, fontWeight: '800' },
});
