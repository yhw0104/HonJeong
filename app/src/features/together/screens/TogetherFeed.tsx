// TogetherFeed — 같이 먹기 탭(하단바). 주변 혼밥 식당 + 받은/보낸 신청.
import React, { useCallback, useState } from 'react';
import { View, Text, Pressable, ScrollView, Alert, StyleSheet, RefreshControl } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Screen } from '@/shared/components';
import { T2 } from '@/shared/theme';
import type { MainTabScreenProps } from '@/navigation/types';
import { useLocation } from '@/shared/location/useLocation';
import { useNearby } from '@/features/place/queries';
import { formatDistance } from '@/shared/format';
import {
  useReceivedRequests, useSentRequests, useAcceptMealRequest, useDeclineMealRequest, useWithdrawMealRequest,
} from '@/features/meal/queries';
import { mealErrorMessage } from '@/features/meal/mealCopy';
import { MealRequestSegments, MealRequestLists, type MealTab } from '@/features/meal/components/MealRequestList';
import { fetchMyCheckIn } from '@/features/checkin/api';
import { nearbyDiningPlaces } from '@/features/place/nearbyDining';
import { LIVE_REFETCH_MS } from '@/shared/realtime';

// 가로 카드 최대 개수 — /places/nearby 한 페이지(20)와 같게 둬서 사실상 전부 보여준다.
const MAX_LIVE_CARDS = 20;

export function TogetherFeedScreen({ navigation }: MainTabScreenProps<'TogetherFeed'>) {
  const [tab, setTab] = useState<MealTab>('received');
  const { coord } = useLocation();
  const nearby = useNearby(coord);
  const received = useReceivedRequests();
  const sent = useSentRequests();
  const accept = useAcceptMealRequest();
  const decline = useDeclineMealRequest();
  const withdraw = useWithdrawMealRequest();
  const qc = useQueryClient();
  const [refreshing, setRefreshing] = useState(false);

  // 이 화면에 있는 동안만 내 체크인을 폴링해 매칭(TOGETHER) 전이를 빠르게 반영한다.
  // useMyCheckIn(다른 화면)과 동일한 쿼리 키를 써서 캐시를 공유하되, 기본 훅 자체는 건드리지 않는다.
  // 내가 모집중인 식당을 인원에서 나만 빼는 데도 쓴다(seekingCount에 내가 포함돼 있어서).
  const myCheckIn = useQuery({
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

  // 같이먹기 탭이므로 "혼자 먹는 중(ACTIVE)"이 아니라 "같이 먹을 사람 구하는 중(SEEKING)"인 식당만 띄운다.
  // 검색 첫 화면과 같은 순수 로직을 재사용 — 기준이 갈리지 않게(가까운 순·본인 제외).
  const livePlaces = nearbyDiningPlaces(
    nearby.data?.content ?? [],
    MAX_LIVE_CARDS,
    myCheckIn.data?.placeId ?? null,
  );
  const receivedList = received.data ?? [];
  const sentList = sent.data ?? [];

  const respond = (mut: typeof accept, id: number) =>
    mut.mutate(id, { onError: (e) => Alert.alert('처리 실패', mealErrorMessage(e)) });

  // 수락만 홈 탭으로 보낸다 — 성사된 뒤 상태(종료·상대 정보)를 다루는 곳이 홈이라
  // 이 목록에 남아 있으면 방금 잡힌 약속이 어디로 갔는지 알 수 없다.
  // 거절·철회는 여기서 계속 처리하므로 그대로 둔다(ReceivedRequests와 같은 규칙).
  const acceptAndGoHome = (id: number) =>
    accept.mutate(id, {
      onSuccess: () => navigation.navigate('MapHome'),
      onError: (e) => Alert.alert('처리 실패', mealErrorMessage(e)),
    });

  return (
    <Screen bg={T2.bg} edges={['top']}>
      <View style={styles.header}>
        <Text style={styles.h1}>같이 먹기</Text>
        <MealRequestSegments tab={tab} onTab={setTab} receivedCount={receivedList.length} sentCount={sentList.length} />
      </View>
      <View style={styles.divider} />

      <ScrollView
        contentContainerStyle={styles.scroll}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={T2.brand} />}
      >
        {/* 주변 모집중 식당 — nearby 재사용 */}
        <View style={styles.liveHead}>
          <View style={styles.liveHeadLeft}>
            <View style={styles.liveDot} />
            <Text style={styles.liveLabel}>지금 모집 중</Text>
          </View>
          <Text style={styles.liveCount}>내 주변 {livePlaces.length}</Text>
        </View>
        {livePlaces.length === 0 ? (
          <Text style={styles.emptyInline}>주변에 모집 중인 식당이 없어요.</Text>
        ) : (
          <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.liveScroll} contentContainerStyle={styles.liveScrollContent}>
            {livePlaces.map((p) => (
              <Pressable key={p.placeId} style={styles.liveCard}
                onPress={() => navigation.navigate('RestaurantDetail', { placeId: p.placeId, name: p.name })}
                accessibilityRole="button">
                <Text style={styles.livePlace} numberOfLines={1}>{p.name}</Text>
                <Text style={styles.liveMeta}>{formatDistance(p.distanceMeters)}</Text>
                <View style={styles.liveBadge}><Text style={styles.liveBadgeText}>{p.seekingCount}명 모집 중</Text></View>
              </Pressable>
            ))}
          </ScrollView>
        )}

        <View style={styles.sectionDivider} />

        <MealRequestLists
          tab={tab}
          receivedList={receivedList}
          receivedLoading={received.isLoading}
          receivedError={received.isError}
          sentList={sentList}
          sentLoading={sent.isLoading}
          sentError={sent.isError}
          onAccept={acceptAndGoHome}
          onDecline={(id) => respond(decline, id)}
          acceptPending={accept.isPending}
          declinePending={decline.isPending}
          onWithdraw={(id) => respond(withdraw, id)}
          withdrawPending={withdraw.isPending}
          onOpenProfile={(userId) => navigation.navigate('MateProfile', { userId })}
          onOpenPlace={(placeId, name) => navigation.navigate('RestaurantDetail', { placeId, name })}
        />
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  header: { paddingHorizontal: 20, paddingTop: 12 },
  h1: { fontSize: 28, fontWeight: '800', color: T2.text, letterSpacing: -1 },
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
});
