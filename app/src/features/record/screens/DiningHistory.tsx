// DiningHistory — 내 혼밥 기록 (원본: screens/DiningHistory.jsx)
// 더보기 '내 혼밥 기록'에서 진입. 요약 통계 + 월별 기록(일기 없는 방문은 '일기 쓰기'→DiningLogWrite).
import React from 'react';
import { View, Text, Pressable, ScrollView, StyleSheet } from 'react-native';
import { Screen, MoreHeader, ImagePlaceholder, Icon } from '@/shared/components';
import { T2 } from '@/shared/theme';
import type { RootStackScreenProps } from '@/navigation/types';

type Entry = {
  d: string;
  day: string;
  place: string;
  note?: string;
  taste?: string;
  honbab?: string;
  photo?: boolean;
  empty?: boolean;
};

const SUMMARY = [
  { n: '32', l: '총 혼밥' },
  { n: '28', l: '일기' },
  { n: '12', l: '식당' },
  { n: '5', l: '이번달' },
];
const MONTHS: { m: string; entries: Entry[] }[] = [
  {
    m: '2026년 5월',
    entries: [
      { d: '22', day: 'FRI', place: '큰순두부 연남점', note: '벽 보고 앉아서 마음 편히 먹었다.', taste: '5.0', honbab: '5.0', photo: true },
      { d: '20', day: 'WED', place: '연남 김밥', empty: true },
      { d: '18', day: 'MON', place: '혼밥의자', note: '바테이블 끝자리. 책 읽으며 30분.', taste: '4.5', honbab: '4.5', photo: true },
      { d: '11', day: 'MON', place: '옥상국밥', note: '점심 빠르게. 1인석 바로 앉음.', taste: '4.0', honbab: '4.0', photo: false },
    ],
  },
  {
    m: '2026년 4월',
    entries: [
      { d: '29', day: 'TUE', place: '연남 파스타바', note: '큰맘 먹고 양식집 혼밥 첫 도전!', taste: '4.5', honbab: '4.0', photo: true },
      { d: '25', day: 'FRI', place: '망원 우동집', empty: true },
      { d: '20', day: 'SUN', place: '큰순두부 연남점', note: '주말 브런치. 한산해서 좋았음.', taste: '5.0', honbab: '5.0', photo: false },
    ],
  },
];

export function DiningHistoryScreen({ navigation }: RootStackScreenProps<'DiningHistory'>) {
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

        {/* 월별 기록 */}
        {MONTHS.map((mo) => (
          <View key={mo.m} style={{ marginTop: 24 }}>
            <Text style={styles.monthTitle}>{mo.m}</Text>
            <View style={{ gap: 10 }}>
              {mo.entries.map((e, ei) =>
                e.empty ? (
                  <View key={ei} style={styles.emptyCard}>
                    <View style={styles.dateCellDim}>
                      <Text style={styles.dateDimNum}>{e.d}</Text>
                      <Text style={styles.dateDay}>{e.day}</Text>
                    </View>
                    <View style={{ flex: 1, minWidth: 0 }}>
                      <Text style={styles.emptyPlace}>{e.place}</Text>
                      <Text style={styles.emptyMeta}>혼밥 기록 · 일기 없음</Text>
                    </View>
                    <Pressable style={styles.writeChip} onPress={() => navigation.navigate('DiningLogWrite', { placeId: 0, placeName: e.place })}>
                      <Icon name="pencil" size={13} color={T2.brand} />
                      <Text style={styles.writeChipText}>일기 쓰기</Text>
                    </Pressable>
                  </View>
                ) : (
                  <View key={ei} style={styles.card}>
                    <View style={styles.dateCell}>
                      <Text style={styles.dateNum}>{e.d}</Text>
                      <Text style={styles.dateDay}>{e.day}</Text>
                    </View>
                    <View style={{ flex: 1, minWidth: 0 }}>
                      <Text style={styles.place}>{e.place}</Text>
                      <Text style={styles.note}>{e.note}</Text>
                      <View style={styles.ratingRow}>
                        <View style={styles.tasteChip}>
                          <Text style={styles.tasteText}>맛 ★ {e.taste}</Text>
                        </View>
                        <View style={styles.honbabChip}>
                          <Text style={styles.honbabText}>혼밥 ★ {e.honbab}</Text>
                        </View>
                      </View>
                    </View>
                    {e.photo ? <ImagePlaceholder w={56} h={56} radius={12} /> : null}
                  </View>
                )
              )}
            </View>
          </View>
        ))}
      </ScrollView>
    </Screen>
  );
}

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
});
