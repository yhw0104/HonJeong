// ReceivedRequests — 받은 같이 먹기 신청(더보기 진입). 같이먹기 탭과 데이터층(meal) 공유.
import React, { useCallback } from 'react';
import { View, Text, Pressable, ScrollView, ActivityIndicator, Alert, StyleSheet } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { Screen, MoreHeader, EmojiCircle, Icon } from '@/shared/components';
import { T2 } from '@/shared/theme';
import type { RootStackScreenProps } from '@/navigation/types';
import { useReceivedRequests, useAcceptMealRequest, useDeclineMealRequest } from '@/features/meal/queries';
import { mealStatusLabel, mealErrorMessage } from '@/features/meal/mealCopy';

export function ReceivedRequestsScreen({ navigation }: RootStackScreenProps<'ReceivedRequests'>) {
  const received = useReceivedRequests();
  const accept = useAcceptMealRequest();
  const decline = useDeclineMealRequest();

  useFocusEffect(useCallback(() => { received.refetch(); }, [received.refetch]));

  const list = received.data ?? [];
  const pending = list.filter((r) => r.status === 'PENDING');
  const past = list.filter((r) => r.status !== 'PENDING');
  const respond = (mut: typeof accept, id: number) =>
    mut.mutate(id, { onError: (e) => Alert.alert('처리 실패', mealErrorMessage(e)) });

  return (
    <Screen bg={T2.bg} edges={['top']}>
      <MoreHeader title="받은 같이 먹기 신청" onBack={() => navigation.goBack()} />
      <ScrollView contentContainerStyle={styles.scroll}>
        {received.isLoading ? (
          <ActivityIndicator color={T2.brand} style={{ marginTop: 20 }} />
        ) : received.isError ? (
          <Text style={styles.empty}>신청을 불러오지 못했어요.</Text>
        ) : (
          <>
            <Text style={styles.label}>새로운 신청 {pending.length}</Text>
            {pending.length === 0 ? (
              <Text style={styles.empty}>처리할 신청이 없어요.</Text>
            ) : (
              <View style={{ gap: 12 }}>
                {pending.map((r) => (
                  <View key={r.mealRequestId} style={styles.card}>
                    <View style={styles.cardHead}>
                      <EmojiCircle emoji={r.fromUser.nickname[0] ?? '?'} size={46} />
                      <View style={{ flex: 1, minWidth: 0 }}>
                        <Text style={styles.name}>{r.fromUser.nickname}</Text>
                      </View>
                    </View>
                    <View style={styles.placeChip}>
                      <Icon name="pin" size={14} color={T2.brand} />
                      <Text style={styles.placeText}>{r.placeName}</Text>
                    </View>
                    {r.message ? <Text style={styles.msg}>"{r.message}"</Text> : null}
                    <View style={styles.btnRow}>
                      <Pressable style={styles.declineBtn} disabled={decline.isPending}
                        onPress={() => respond(decline, r.mealRequestId)} accessibilityRole="button">
                        <Text style={styles.declineText}>거절</Text>
                      </Pressable>
                      <Pressable style={styles.acceptBtn} disabled={accept.isPending}
                        onPress={() => respond(accept, r.mealRequestId)} accessibilityRole="button">
                        <Text style={styles.acceptText}>수락하기</Text>
                      </Pressable>
                    </View>
                  </View>
                ))}
              </View>
            )}

            <Text style={[styles.label, { marginTop: 28 }]}>지난 신청</Text>
            {past.length === 0 ? (
              <Text style={styles.empty}>지난 신청이 없어요.</Text>
            ) : (
              <View>
                {past.map((p, i) => (
                  <View key={p.mealRequestId} style={[styles.pastRow, i < past.length - 1 && styles.pastDivider]}>
                    <EmojiCircle emoji={p.fromUser.nickname[0] ?? '?'} size={40} dimmed />
                    <View style={{ flex: 1, minWidth: 0 }}>
                      <Text style={styles.pastName}>{p.fromUser.nickname}</Text>
                      <Text style={styles.pastPlace}>{p.placeName}</Text>
                    </View>
                    <Text style={styles.pastState}>{mealStatusLabel(p.status)}</Text>
                  </View>
                ))}
              </View>
            )}
          </>
        )}
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  scroll: { paddingHorizontal: 20, paddingTop: 8, paddingBottom: 40 },
  label: { fontSize: 11, fontWeight: '700', color: T2.textMute, letterSpacing: 0.6, marginBottom: 12 },
  empty: { fontSize: 13, color: T2.textMute, paddingVertical: 8 },
  card: { padding: 18, backgroundColor: '#fff', borderRadius: 18, borderWidth: 1, borderColor: T2.border },
  cardHead: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  name: { fontSize: 15, fontWeight: '800', color: T2.text, letterSpacing: -0.3 },
  placeChip: { flexDirection: 'row', alignItems: 'center', gap: 7, marginTop: 14, paddingVertical: 8, paddingHorizontal: 12, borderRadius: 10, backgroundColor: T2.brandSoft, alignSelf: 'flex-start' },
  placeText: { fontSize: 12, fontWeight: '700', color: T2.brand, letterSpacing: -0.2 },
  msg: { fontSize: 13, color: T2.textSub, lineHeight: 21, marginTop: 12, letterSpacing: -0.3 },
  btnRow: { flexDirection: 'row', gap: 8, marginTop: 16 },
  declineBtn: { flex: 1, paddingVertical: 13, borderRadius: 11, backgroundColor: T2.bg, alignItems: 'center' },
  declineText: { fontSize: 14, fontWeight: '700', color: T2.textSub, letterSpacing: -0.3 },
  acceptBtn: { flex: 2, paddingVertical: 13, borderRadius: 11, backgroundColor: T2.brand, alignItems: 'center' },
  acceptText: { fontSize: 14, fontWeight: '700', color: '#fff', letterSpacing: -0.3 },
  pastRow: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 14 },
  pastDivider: { borderBottomWidth: 1, borderBottomColor: T2.border },
  pastName: { fontSize: 14, fontWeight: '700', color: T2.textSub, letterSpacing: -0.3 },
  pastPlace: { fontSize: 11, color: T2.textMute, marginTop: 3 },
  pastState: { fontSize: 12, fontWeight: '600', color: T2.textMute },
});
