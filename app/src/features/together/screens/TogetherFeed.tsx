// TogetherFeed — 같이 먹기 탭(하단바). 주변 혼밥 식당 + 받은/보낸 신청.
import React, { useCallback, useState } from 'react';
import { View, Text, Pressable, ScrollView, ActivityIndicator, Alert, StyleSheet, RefreshControl } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Screen, EmojiCircle, Icon } from '@/shared/components';
import { T2 } from '@/shared/theme';
import type { MainTabScreenProps } from '@/navigation/types';
import { useLocation } from '@/shared/location/useLocation';
import { useNearby } from '@/features/place/queries';
import { formatDistance } from '@/shared/format';
import {
  useReceivedRequests, useSentRequests, useAcceptMealRequest, useDeclineMealRequest,
} from '@/features/meal/queries';
import { mealStatusLabelReceived, mealStatusLabelSent, mealErrorMessage } from '@/features/meal/mealCopy';
import { fetchMyCheckIn } from '@/features/checkin/api';
import { LIVE_REFETCH_MS } from '@/shared/realtime';

export function TogetherFeedScreen({ navigation }: MainTabScreenProps<'TogetherFeed'>) {
  const [tab, setTab] = useState<'received' | 'sent'>('received');
  const { coord } = useLocation();
  const nearby = useNearby(coord);
  const received = useReceivedRequests();
  const sent = useSentRequests();
  const accept = useAcceptMealRequest();
  const decline = useDeclineMealRequest();
  const qc = useQueryClient();
  const [refreshing, setRefreshing] = useState(false);

  // 이 화면에 있는 동안만 내 체크인을 폴링해 매칭(TOGETHER) 전이를 빠르게 반영한다.
  // useMyCheckIn(다른 화면)과 동일한 쿼리 키를 써서 캐시를 공유하되, 기본 훅 자체는 건드리지 않는다.
  useQuery({
    queryKey: ['checkin', 'me'],
    queryFn: fetchMyCheckIn,
    refetchInterval: LIVE_REFETCH_MS,
  });

  useFocusEffect(useCallback(() => { received.refetch(); sent.refetch(); }, [received.refetch, sent.refetch]));

  const onRefresh = useCallback(async () => {
    setRefreshing(true);
    await Promise.all([
      qc.invalidateQueries({ queryKey: ['meal'] }),
      qc.invalidateQueries({ queryKey: ['checkin', 'me'] }),
    ]);
    setRefreshing(false);
  }, [qc]);

  const livePlaces = (nearby.data?.content ?? []).filter((p) => p.activeCount > 0);
  const receivedList = received.data ?? [];
  const sentList = sent.data ?? [];

  const respond = (mut: typeof accept, id: number) =>
    mut.mutate(id, { onError: (e) => Alert.alert('처리 실패', mealErrorMessage(e)) });

  return (
    <Screen bg={T2.bg} edges={['top']}>
      <View style={styles.header}>
        <Text style={styles.h1}>같이 먹기</Text>
        <View style={styles.segRow}>
          {([
            { key: 'received' as const, label: '받은 신청', count: receivedList.length },
            { key: 'sent' as const, label: '보낸 신청', count: sentList.length },
          ]).map((s) => {
            const on = tab === s.key;
            return (
              <Pressable key={s.key} style={styles.seg} onPress={() => setTab(s.key)} accessibilityRole="button">
                <Text style={[styles.segLabel, { color: on ? T2.text : T2.textMute }]}>{s.label}</Text>
                <Text style={[styles.segCount, { color: on ? T2.brand : T2.textMute }]}>{s.count}</Text>
                {on && <View style={styles.segUnderline} />}
              </Pressable>
            );
          })}
        </View>
      </View>
      <View style={styles.divider} />

      <ScrollView
        contentContainerStyle={styles.scroll}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={T2.brand} />}
      >
        {/* 주변 혼밥 식당 — nearby 재사용 */}
        <View style={styles.liveHead}>
          <View style={styles.liveHeadLeft}>
            <View style={styles.liveDot} />
            <Text style={styles.liveLabel}>지금 주변에 혼밥러가 있어요</Text>
          </View>
          <Text style={styles.liveCount}>내 주변 {livePlaces.length}</Text>
        </View>
        {livePlaces.length === 0 ? (
          <Text style={styles.emptyInline}>주변에 혼밥 중인 식당이 아직 없어요.</Text>
        ) : (
          <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.liveScroll} contentContainerStyle={styles.liveScrollContent}>
            {livePlaces.map((p) => (
              <Pressable key={p.placeId} style={styles.liveCard}
                onPress={() => navigation.navigate('RestaurantDetail', { placeId: p.placeId, name: p.name })}
                accessibilityRole="button">
                <Text style={styles.livePlace} numberOfLines={1}>{p.name}</Text>
                <Text style={styles.liveMeta}>{formatDistance(p.distanceMeters)}</Text>
                <View style={styles.liveBadge}><Text style={styles.liveBadgeText}>{p.activeCount}명 혼밥 중</Text></View>
              </Pressable>
            ))}
          </ScrollView>
        )}

        <View style={styles.sectionDivider} />

        {/* 받은 신청 */}
        {tab === 'received' && (
          received.isLoading ? <ActivityIndicator color={T2.brand} /> :
          received.isError ? <Text style={styles.emptyInline}>신청을 불러오지 못했어요.</Text> :
          receivedList.length === 0 ? <Text style={styles.emptyInline}>받은 신청이 없어요.</Text> : (
            <View style={{ gap: 12 }}>
              {receivedList.map((r) => (
                <View key={r.mealRequestId} style={styles.recvCard}>
                  <Pressable style={styles.recvTop} onPress={() => navigation.navigate('MateProfile', { userId: r.fromUser.userId })} accessibilityRole="button">
                    <EmojiCircle emoji={r.fromUser.nickname[0] ?? '?'} size={46} />
                    <View style={{ flex: 1, minWidth: 0 }}>
                      <Text style={styles.recvName}>{r.fromUser.nickname}</Text>
                      <Text style={styles.recvMeta}>{r.status === 'PENDING' ? '새 신청' : mealStatusLabelReceived(r.status)}</Text>
                    </View>
                  </Pressable>
                  <View style={styles.recvPlace}>
                    <Icon name="pin" size={14} color={T2.brand} />
                    <Text style={styles.recvPlaceText}>{r.placeName}</Text>
                  </View>
                  {r.message ? <Text style={styles.recvMsg}>"{r.message}"</Text> : null}
                  {r.status === 'PENDING' && (
                    <View style={styles.recvBtns}>
                      <Pressable style={[styles.recvBtn, styles.recvBtnReject]} disabled={decline.isPending}
                        onPress={() => respond(decline, r.mealRequestId)} accessibilityRole="button">
                        <Text style={styles.recvBtnRejectText}>거절</Text>
                      </Pressable>
                      <Pressable style={[styles.recvBtn, styles.recvBtnAccept]} disabled={accept.isPending}
                        onPress={() => respond(accept, r.mealRequestId)} accessibilityRole="button">
                        <Text style={styles.recvBtnAcceptText}>수락하기</Text>
                      </Pressable>
                    </View>
                  )}
                </View>
              ))}
            </View>
          )
        )}

        {/* 보낸 신청 */}
        {tab === 'sent' && (
          sent.isLoading ? <ActivityIndicator color={T2.brand} /> :
          sent.isError ? <Text style={styles.emptyInline}>신청을 불러오지 못했어요.</Text> :
          sentList.length === 0 ? <Text style={styles.emptyInline}>보낸 신청이 없어요.</Text> : (
            <View>
              {sentList.map((s, i) => (
                <Pressable key={s.mealRequestId} style={[styles.sentRow, i < sentList.length - 1 && styles.sentRowBorder]} onPress={() => navigation.navigate('MateProfile', { userId: s.toUser.userId })} accessibilityRole="button">
                  <EmojiCircle emoji={s.toUser.nickname[0] ?? '?'} size={44} dimmed={s.status === 'DECLINED'} />
                  <View style={{ flex: 1, minWidth: 0 }}>
                    <Text style={[styles.sentName, { color: s.status === 'DECLINED' ? T2.textMute : T2.text }]}>{s.toUser.nickname}</Text>
                    <Text style={styles.sentMeta} numberOfLines={1}>{s.placeName}</Text>
                  </View>
                  <View style={[styles.sentPill, { backgroundColor: s.status === 'ACCEPTED' ? T2.brandSoft : T2.bg, borderColor: s.status === 'ACCEPTED' ? 'rgba(255,90,31,0.2)' : T2.border }]}>
                    <Text style={[styles.sentPillText, { color: s.status === 'ACCEPTED' ? T2.brand : T2.textMute }]}>{mealStatusLabelSent(s.status)}</Text>
                  </View>
                </Pressable>
              ))}
            </View>
          )
        )}
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  header: { paddingHorizontal: 20, paddingTop: 12 },
  h1: { fontSize: 28, fontWeight: '800', color: T2.text, letterSpacing: -1 },
  segRow: { flexDirection: 'row', gap: 22, marginTop: 16 },
  seg: { flexDirection: 'row', alignItems: 'center', gap: 6, paddingBottom: 12 },
  segLabel: { fontSize: 16, fontWeight: '800', letterSpacing: -0.3 },
  segCount: { fontSize: 12, fontWeight: '700' },
  segUnderline: { position: 'absolute', left: 0, right: 0, bottom: 0, height: 2, backgroundColor: T2.brand },
  divider: { height: 1, backgroundColor: T2.border },
  scroll: { paddingHorizontal: 20, paddingTop: 16, paddingBottom: 32 },
  liveHead: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 },
  liveHeadLeft: { flexDirection: 'row', alignItems: 'center', gap: 7 },
  liveDot: { width: 7, height: 7, borderRadius: 3.5, backgroundColor: T2.brand },
  liveLabel: { fontSize: 11, fontWeight: '700', color: T2.text, letterSpacing: 0.6 },
  liveCount: { fontSize: 12, fontWeight: '700', color: T2.textMute },
  emptyInline: { fontSize: 13, color: T2.textMute, paddingVertical: 10 },
  liveScroll: { marginHorizontal: -20 },
  liveScrollContent: { paddingHorizontal: 20, paddingBottom: 4, gap: 10 },
  liveCard: { width: 156, padding: 14, backgroundColor: '#fff', borderRadius: 16, borderWidth: 1, borderColor: T2.border },
  livePlace: { fontSize: 14, fontWeight: '800', color: T2.text, letterSpacing: -0.3 },
  liveMeta: { fontSize: 11.5, color: T2.textMute, marginTop: 6 },
  liveBadge: { alignSelf: 'flex-start', marginTop: 10, paddingHorizontal: 9, paddingVertical: 5, borderRadius: 9, backgroundColor: T2.brandSoft },
  liveBadgeText: { fontSize: 12, fontWeight: '700', color: T2.brand, letterSpacing: -0.3 },
  sectionDivider: { height: 1, backgroundColor: T2.border, marginTop: 22, marginBottom: 18 },
  recvCard: { padding: 18, backgroundColor: '#fff', borderRadius: 18, borderWidth: 1, borderColor: T2.border },
  recvTop: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  recvName: { fontSize: 15, fontWeight: '800', color: T2.text, letterSpacing: -0.3 },
  recvMeta: { fontSize: 11, color: T2.textMute, marginTop: 3 },
  recvPlace: { flexDirection: 'row', alignItems: 'center', gap: 7, marginTop: 14, paddingVertical: 8, paddingHorizontal: 12, borderRadius: 10, backgroundColor: T2.brandSoft, alignSelf: 'flex-start' },
  recvPlaceText: { fontSize: 12, fontWeight: '700', color: T2.brand, letterSpacing: -0.2 },
  recvMsg: { fontSize: 13, color: T2.textSub, lineHeight: 21, marginTop: 12, letterSpacing: -0.3 },
  recvBtns: { flexDirection: 'row', gap: 8, marginTop: 16 },
  recvBtn: { paddingVertical: 13, borderRadius: 11, alignItems: 'center' },
  recvBtnReject: { flex: 1, backgroundColor: T2.bg },
  recvBtnRejectText: { fontSize: 14, fontWeight: '700', color: T2.textSub, letterSpacing: -0.3 },
  recvBtnAccept: { flex: 2, backgroundColor: T2.brand },
  recvBtnAcceptText: { fontSize: 14, fontWeight: '700', color: '#fff', letterSpacing: -0.3 },
  sentRow: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 15 },
  sentRowBorder: { borderBottomWidth: 1, borderBottomColor: T2.border },
  sentName: { fontSize: 14.5, fontWeight: '700', letterSpacing: -0.3 },
  sentMeta: { fontSize: 12, color: T2.textMute, marginTop: 3 },
  sentPill: { paddingVertical: 7, paddingHorizontal: 12, borderRadius: 999, borderWidth: 1 },
  sentPillText: { fontSize: 12, fontWeight: '700', letterSpacing: -0.2 },
});
