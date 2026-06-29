// DiningHistory — 내 혼밥 기록 (원본: screens/DiningHistory.jsx)
// 더보기 '내 혼밥 기록'에서 진입. 요약 통계 + 월별 기록(일기 없는 방문은 '일기 쓰기'→DiningLogWrite).
import React from 'react';
import { View, Text, Pressable, ScrollView, StyleSheet, ActivityIndicator, Alert } from 'react-native';
import { Screen, MoreHeader, Icon } from '@/shared/components';
import { T2 } from '@/shared/theme';
import type { RootStackScreenProps } from '@/navigation/types';
import { useDiningHistory, useDeleteReview } from '@/features/review/queries';

export function DiningHistoryScreen({ navigation }: RootStackScreenProps<'DiningHistory'>) {
  const { data, isLoading, isError } = useDiningHistory();
  const delMut = useDeleteReview();

  const confirmDelete = (reviewId: number) =>
    Alert.alert('리뷰 삭제', '이 리뷰를 삭제할까요? 되돌릴 수 없어요.', [
      { text: '취소', style: 'cancel' },
      { text: '삭제', style: 'destructive', onPress: () => delMut.mutate(reviewId) },
    ]);

  const groups = groupByMonth(data?.entries ?? []);
  const summary = data?.summary;

  const SUMMARY = summary
    ? [
        { n: String(summary.totalCheckIns), l: '총 혼밥' },
        { n: String(summary.totalReviews), l: '일기' },
        { n: String(summary.distinctPlaces), l: '식당' },
        { n: String(summary.thisMonthCheckIns), l: '이번달' },
      ]
    : [];

  if (isLoading)
    return (
      <Screen bg={T2.bg} edges={['top']}>
        <MoreHeader title="내 혼밥 기록" onBack={() => navigation.goBack()} />
        <View style={{ padding: 40, alignItems: 'center' }}>
          <ActivityIndicator color={T2.brand} />
        </View>
      </Screen>
    );

  if (isError)
    return (
      <Screen bg={T2.bg} edges={['top']}>
        <MoreHeader title="내 혼밥 기록" onBack={() => navigation.goBack()} />
        <View style={{ padding: 40 }}>
          <Text style={{ color: T2.textMute }}>기록을 불러오지 못했어요.</Text>
        </View>
      </Screen>
    );

  return (
    <Screen bg={T2.bg} edges={['top']}>
      <MoreHeader title="내 혼밥 기록" onBack={() => navigation.goBack()} />

      <ScrollView contentContainerStyle={styles.scroll}>
        {/* 요약 통계 */}
        <View style={styles.summary}>
          {SUMMARY.map((s, i) => (
            <View key={s.l} style={[styles.statCell, i > 0 && styles.statDivider]}>
              <Text style={[styles.statNum, { color: i === 3 ? T2.brand : T2.text }]}>{s.n}</Text>
              <Text style={styles.statLabel}>{s.l}</Text>
            </View>
          ))}
        </View>

        {/* 빈 상태 */}
        {groups.length === 0 && (
          <Text style={{ padding: 24, color: T2.textMute, textAlign: 'center' }}>아직 혼밥 기록이 없어요.</Text>
        )}

        {/* 월별 기록 */}
        {groups.map((group) => (
          <View key={group.m} style={{ marginTop: 24 }}>
            <Text style={styles.monthTitle}>{group.m}</Text>
            <View style={{ gap: 10 }}>
              {group.items.map((e) => {
                const { d, day } = dayParts(e.visitedAt);
                const openPlace = () =>
                  navigation.navigate('RestaurantDetail', { placeId: e.placeId, name: e.placeName });
                return e.review == null ? (
                  <Pressable key={e.checkInId} style={styles.emptyCard} onPress={openPlace}>
                    <View style={styles.dateCellDim}>
                      <Text style={styles.dateDimNum}>{d}</Text>
                      <Text style={styles.dateDay}>{day}</Text>
                    </View>
                    <View style={{ flex: 1, minWidth: 0 }}>
                      <Text style={styles.emptyPlace}>{e.placeName}</Text>
                      <Text style={styles.emptyMeta}>혼밥 기록 · 일기 없음</Text>
                    </View>
                    <Pressable
                      style={styles.writeChip}
                      onPress={() =>
                        navigation.navigate('DiningLogWrite', {
                          placeId: e.placeId,
                          placeName: e.placeName,
                          checkInId: e.checkInId,
                        })
                      }
                    >
                      <Icon name="pencil" size={13} color={T2.brand} />
                      <Text style={styles.writeChipText}>일기 쓰기</Text>
                    </Pressable>
                  </Pressable>
                ) : (
                  <Pressable key={e.checkInId} style={styles.card} onPress={openPlace}>
                    <View style={styles.dateCell}>
                      <Text style={styles.dateNum}>{d}</Text>
                      <Text style={styles.dateDay}>{day}</Text>
                    </View>
                    <View style={{ flex: 1, minWidth: 0 }}>
                      <Text style={styles.place}>{e.placeName}</Text>
                      {e.review.content ? (
                        <Text style={styles.note}>{e.review.content}</Text>
                      ) : null}
                      <View style={styles.ratingRow}>
                        <View style={styles.tasteChip}>
                          <Text style={styles.tasteText}>맛 ★ {e.review.tasteRating.toFixed(1)}</Text>
                        </View>
                        <View style={styles.honbabChip}>
                          <Text style={styles.honbabText}>혼밥 ★ {e.review.soloFriendlyRating.toFixed(1)}</Text>
                        </View>
                      </View>
                      <View style={styles.actionRow}>
                        <Pressable
                          hitSlop={6}
                          onPress={() =>
                            navigation.navigate('DiningLogWrite', {
                              placeId: e.placeId,
                              placeName: e.placeName,
                              checkInId: e.checkInId,
                              reviewId: e.review!.reviewId,
                              initial: {
                                taste: e.review!.tasteRating,
                                honbab: e.review!.soloFriendlyRating,
                                tags: e.review!.tags,
                                content: e.review!.content ?? '',
                                photos: e.review!.imageUrls,
                              },
                            })
                          }
                        >
                          <Text style={styles.actionEdit}>수정</Text>
                        </Pressable>
                        <Pressable hitSlop={6} onPress={() => confirmDelete(e.review!.reviewId)}>
                          <Text style={styles.actionDelete}>삭제</Text>
                        </Pressable>
                      </View>
                    </View>
                  </Pressable>
                );
              })}
            </View>
          </View>
        ))}
      </ScrollView>
    </Screen>
  );
}

// ── 헬퍼 ──────────────────────────────────────────────────────────────────────

function groupByMonth(entries: { visitedAt: string }[] & any[]) {
  const map = new Map<string, any[]>();
  for (const e of entries) {
    const d = new Date(e.visitedAt);
    const key = `${d.getFullYear()}년 ${d.getMonth() + 1}월`;
    if (!map.has(key)) map.set(key, []);
    map.get(key)!.push(e);
  }
  return Array.from(map, ([m, items]) => ({ m, items }));
}

function dayParts(visitedAt: string) {
  const d = new Date(visitedAt);
  const days = ['SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT'];
  return { d: String(d.getDate()), day: days[d.getDay()] };
}

// ── 스타일 ────────────────────────────────────────────────────────────────────

const styles = StyleSheet.create({
  scroll: { paddingHorizontal: 20, paddingTop: 8, paddingBottom: 40 },

  summary: { flexDirection: 'row', paddingTop: 18, paddingBottom: 22, borderBottomWidth: 1, borderBottomColor: T2.border },
  statCell: { flex: 1, alignItems: 'center' },
  statDivider: { borderLeftWidth: 1, borderLeftColor: T2.border },
  statNum: { fontSize: 24, fontWeight: '800', letterSpacing: -0.8 },
  statLabel: { fontSize: 11, color: T2.textMute, marginTop: 3 },

  monthTitle: { fontSize: 13, fontWeight: '800', color: T2.text, letterSpacing: -0.3, marginBottom: 12 },

  card: { flexDirection: 'row', gap: 14, padding: 14, backgroundColor: '#fff', borderRadius: 16, borderWidth: 1, borderColor: T2.border },
  dateCell: { width: 40, alignItems: 'center' },
  dateNum: { fontSize: 20, fontWeight: '800', color: T2.text, letterSpacing: -0.5 },
  dateDay: { fontSize: 10, fontWeight: '700', color: T2.textMute, marginTop: 3, letterSpacing: 0.5 },
  place: { fontSize: 14, fontWeight: '700', color: T2.text, letterSpacing: -0.3 },
  note: { fontSize: 12, color: T2.textSub, lineHeight: 18, marginTop: 5, letterSpacing: -0.2 },
  ratingRow: { flexDirection: 'row', gap: 6, marginTop: 10 },
  tasteChip: { backgroundColor: T2.bg, borderWidth: 1, borderColor: T2.border, paddingHorizontal: 7, paddingVertical: 3, borderRadius: 6 },
  tasteText: { fontSize: 11, fontWeight: '700', color: T2.textSub },
  honbabChip: { backgroundColor: T2.brandSoft, paddingHorizontal: 7, paddingVertical: 3, borderRadius: 6 },
  honbabText: { fontSize: 11, fontWeight: '700', color: T2.brand },

  emptyCard: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 14,
    paddingVertical: 12,
    paddingHorizontal: 14,
    borderRadius: 16,
    borderWidth: 1,
    borderStyle: 'dashed',
    borderColor: T2.borderStrong,
  },
  dateCellDim: { width: 40, alignItems: 'center', opacity: 0.55 },
  dateDimNum: { fontSize: 20, fontWeight: '800', color: T2.textSub, letterSpacing: -0.5 },
  emptyPlace: { fontSize: 14, fontWeight: '700', color: T2.textSub, letterSpacing: -0.3 },
  emptyMeta: { fontSize: 12, color: T2.textMute, marginTop: 4, letterSpacing: -0.2 },
  writeChip: { flexDirection: 'row', alignItems: 'center', gap: 5, paddingHorizontal: 12, paddingVertical: 8, borderRadius: 9, backgroundColor: T2.brandSoft },
  writeChipText: { fontSize: 12, fontWeight: '700', color: T2.brand, letterSpacing: -0.2 },

  actionRow: { flexDirection: 'row', gap: 16, marginTop: 10 },
  actionEdit: { fontSize: 12, fontWeight: '700', color: T2.textSub },
  actionDelete: { fontSize: 12, fontWeight: '700', color: '#d11' },
});
