// ReceivedRequests — 더보기 '같이 먹기 신청'. 받은/보낸 토글. 같이먹기 탭과 동일한 디자인(MealRequestList 공용).
import React, { useCallback, useState } from 'react';
import { View, ScrollView, Alert, StyleSheet } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { Screen, MoreHeader } from '@/shared/components';
import { T2 } from '@/shared/theme';
import type { RootStackScreenProps } from '@/navigation/types';
import { useReceivedRequests, useSentRequests, useAcceptMealRequest, useDeclineMealRequest } from '@/features/meal/queries';
import { mealErrorMessage } from '@/features/meal/mealCopy';
import { MealRequestSegments, MealRequestLists, type MealTab } from '@/features/meal/components/MealRequestList';

export function ReceivedRequestsScreen({ navigation }: RootStackScreenProps<'ReceivedRequests'>) {
  const [tab, setTab] = useState<MealTab>('received');
  const received = useReceivedRequests();
  const sent = useSentRequests();
  const accept = useAcceptMealRequest();
  const decline = useDeclineMealRequest();

  useFocusEffect(useCallback(() => { received.refetch(); sent.refetch(); }, [received.refetch, sent.refetch]));

  const receivedList = received.data ?? [];
  const sentList = sent.data ?? [];
  const respond = (mut: typeof accept, id: number) =>
    mut.mutate(id, { onError: (e) => Alert.alert('처리 실패', mealErrorMessage(e)) });

  return (
    <Screen bg={T2.bg} edges={['top']}>
      <MoreHeader title="같이 먹기 신청" onBack={() => navigation.goBack()} />

      <View style={styles.segWrap}>
        <MealRequestSegments tab={tab} onTab={setTab} receivedCount={receivedList.length} sentCount={sentList.length} />
      </View>
      <View style={styles.divider} />

      <ScrollView contentContainerStyle={styles.scroll}>
        <MealRequestLists
          tab={tab}
          receivedList={receivedList}
          receivedLoading={received.isLoading}
          receivedError={received.isError}
          sentList={sentList}
          sentLoading={sent.isLoading}
          sentError={sent.isError}
          onAccept={(id) => respond(accept, id)}
          onDecline={(id) => respond(decline, id)}
          acceptPending={accept.isPending}
          declinePending={decline.isPending}
          onOpenProfile={(userId) => navigation.navigate('MateProfile', { userId })}
        />
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  segWrap: { paddingHorizontal: 20 },
  divider: { height: 1, backgroundColor: T2.border },
  scroll: { paddingHorizontal: 20, paddingTop: 16, paddingBottom: 40 },
});
