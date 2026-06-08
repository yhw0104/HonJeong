// ReceivedRequests — 받은 같이 먹기 신청 (원본: screens/ReceivedRequests.jsx)
// 더보기 '받은 같이 먹기 신청'에서 진입. 새 신청(수락/거절로 카드 제거) + 지난 신청.
import React, { useState } from 'react';
import { View, Text, Pressable, ScrollView, StyleSheet } from 'react-native';
import { Screen, MoreHeader, EmojiCircle, Icon } from '@/shared/components';
import { T2 } from '@/shared/theme';
import type { RootStackScreenProps } from '@/navigation/types';

type Req = { name: string; emo: string; place: string; time: string; meta: string; msg: string };

const INITIAL: Req[] = [
  { name: '점심혼밥러', emo: '🍙', place: '큰순두부 연남점', time: '오늘 12:30', meta: '혼밥 32회 · 같이 먹은 적 2회', msg: '저도 순두부 좋아해요! 같이 조용히 먹어요 :)' },
  { name: '연남책방지기', emo: '📚', place: '혼밥의자', time: '오늘 13:00', meta: '혼밥 12회 · 첫 매칭', msg: '바테이블 옆자리 어떠세요?' },
];
const PAST = [
  { name: '조용한미식가', emo: '🍜', place: '옥상국밥', state: '함께 먹음' },
  { name: '국밥러버', emo: '🍲', place: '큰순두부 연남점', state: '지난 신청' },
];

export function ReceivedRequestsScreen({ navigation }: RootStackScreenProps<'ReceivedRequests'>) {
  const [reqs, setReqs] = useState<Req[]>(INITIAL);
  const remove = (name: string) => setReqs((prev) => prev.filter((r) => r.name !== name));

  return (
    <Screen bg={T2.bg} edges={['top']}>
      <MoreHeader title="받은 같이 먹기 신청" onBack={() => navigation.goBack()} />

      <ScrollView contentContainerStyle={styles.scroll}>
        {/* 새 신청 */}
        <Text style={styles.label}>새로운 신청 {reqs.length}</Text>
        {reqs.length === 0 ? (
          <Text style={styles.empty}>처리할 신청이 없어요.</Text>
        ) : (
          <View style={{ gap: 12 }}>
            {reqs.map((r) => (
              <View key={r.name} style={styles.card}>
                <View style={styles.cardHead}>
                  <EmojiCircle emoji={r.emo} size={46} />
                  <View style={{ flex: 1, minWidth: 0 }}>
                    <Text style={styles.name}>{r.name}</Text>
                    <Text style={styles.meta}>{r.meta}</Text>
                  </View>
                  <Text style={styles.time}>{r.time}</Text>
                </View>

                <View style={styles.placeChip}>
                  <Icon name="pin" size={14} color={T2.brand} />
                  <Text style={styles.placeText}>{r.place}</Text>
                </View>
                <Text style={styles.msg}>"{r.msg}"</Text>

                <View style={styles.btnRow}>
                  <Pressable style={styles.declineBtn} onPress={() => remove(r.name)}>
                    <Text style={styles.declineText}>거절</Text>
                  </Pressable>
                  <Pressable style={styles.acceptBtn} onPress={() => remove(r.name)}>
                    <Text style={styles.acceptText}>수락하기</Text>
                  </Pressable>
                </View>
              </View>
            ))}
          </View>
        )}

        {/* 지난 신청 */}
        <Text style={[styles.label, { marginTop: 28 }]}>지난 신청</Text>
        <View>
          {PAST.map((p, i) => (
            <View key={p.name} style={[styles.pastRow, i < PAST.length - 1 && styles.pastDivider]}>
              <EmojiCircle emoji={p.emo} size={40} dimmed />
              <View style={{ flex: 1, minWidth: 0 }}>
                <Text style={styles.pastName}>{p.name}</Text>
                <Text style={styles.pastPlace}>{p.place}</Text>
              </View>
              <Text style={styles.pastState}>{p.state}</Text>
            </View>
          ))}
        </View>
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
  meta: { fontSize: 11, color: T2.textMute, marginTop: 3 },
  time: { fontSize: 11, color: T2.textMute },

  placeChip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 7,
    marginTop: 14,
    paddingVertical: 8,
    paddingHorizontal: 12,
    borderRadius: 10,
    backgroundColor: T2.brandSoft,
    alignSelf: 'flex-start',
  },
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
