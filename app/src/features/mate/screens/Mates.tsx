// Mates — 메이트 목록 (원본: screens/Mates.jsx)
// 더보기 프로필 '메이트' 스탯에서 진입. 내 메이트 + 알 수도 있는 메이트.
import React, { useState } from 'react';
import { View, Text, Pressable, ScrollView, StyleSheet } from 'react-native';
import { Screen, MoreHeader, EmojiCircle, Icon } from '@/shared/components';
import { T2, C } from '@/shared/theme';
import type { RootStackScreenProps } from '@/navigation/types';

const MY_MATES = [
  { name: '점심혼밥러', emo: '🍙', meta: '연남동 · 혼밥 32회', tags: ['한식', '대화 OK'], together: 2, now: true, nowPlace: '큰순두부 연남점' },
  { name: '조용한미식가', emo: '🍜', meta: '합정 · 혼밥 18회', tags: ['일식', '조용히'], together: 1, now: false, nowPlace: '' },
  { name: '연남책방지기', emo: '📚', meta: '연남동 · 혼밥 12회', tags: ['면 요리', '대화 OK'], together: 0, now: true, nowPlace: '혼밥의자' },
];
const SUGGEST = [
  { name: '국밥러버', emo: '🍲', meta: '망원 · 혼밥 41회', mutual: 3, sent: false },
  { name: '디저트헌터', emo: '🍰', meta: '상수 · 혼밥 9회', mutual: 1, sent: true },
];

export function MatesScreen({ navigation }: RootStackScreenProps<'Mates'>) {
  const [sent, setSent] = useState<string[]>(SUGGEST.filter((s) => s.sent).map((s) => s.name));

  return (
    <Screen bg={T2.bg} edges={['top']}>
      <MoreHeader title="메이트" onBack={() => navigation.goBack()} />

      <ScrollView contentContainerStyle={styles.scroll}>
        {/* 검색 */}
        <View style={styles.search}>
          <Icon name="search" size={18} color={T2.textMute} />
          <Text style={styles.searchText}>이름으로 메이트 찾기</Text>
        </View>

        {/* 내 메이트 */}
        <Text style={styles.label}>내 메이트 {MY_MATES.length}</Text>
        <View style={{ gap: 10 }}>
          {MY_MATES.map((m) => (
            <Pressable
              key={m.name}
              style={styles.card}
              onPress={() => navigation.navigate('MateProfile', { name: m.name })}
            >
              <EmojiCircle emoji={m.emo} size={48} online={m.now} />
              <View style={{ flex: 1, minWidth: 0 }}>
                <View style={styles.nameRow}>
                  <Text style={styles.name}>{m.name}</Text>
                  {m.together > 0 ? (
                    <View style={styles.togetherBadge}>
                      <Text style={styles.togetherText}>같이 {m.together}회</Text>
                    </View>
                  ) : null}
                </View>
                {m.now ? (
                  <View style={styles.nowRow}>
                    <View style={styles.nowDot} />
                    <Text style={styles.nowText}>지금 혼밥 중</Text>
                    <Text style={styles.nowPlace} numberOfLines={1}>
                      · {m.nowPlace}
                    </Text>
                  </View>
                ) : (
                  <Text style={styles.meta}>{m.meta}</Text>
                )}
                <View style={styles.tagRow}>
                  {m.tags.map((t) => (
                    <View key={t} style={styles.tag}>
                      <Text style={styles.tagText}>{t}</Text>
                    </View>
                  ))}
                </View>
              </View>
              <View style={styles.mateChip}>
                <Text style={{ fontSize: 11, fontWeight: '800', color: T2.textSub }}>✓</Text>
                <Text style={styles.mateChipText}>메이트</Text>
              </View>
            </Pressable>
          ))}
        </View>

        {/* 알 수도 있는 메이트 */}
        <Text style={[styles.label, { marginTop: 28 }]}>알 수도 있는 메이트</Text>
        <View style={{ gap: 10 }}>
          {SUGGEST.map((m) => {
            const isSent = sent.includes(m.name);
            return (
              <View key={m.name} style={styles.card}>
                <EmojiCircle emoji={m.emo} size={48} />
                <View style={{ flex: 1, minWidth: 0 }}>
                  <Text style={styles.name}>{m.name}</Text>
                  <Text style={styles.meta}>{m.meta}</Text>
                  <Text style={styles.mutual}>함께 아는 메이트 {m.mutual}명</Text>
                </View>
                <Pressable
                  onPress={() => setSent((prev) => (prev.includes(m.name) ? prev : [...prev, m.name]))}
                  style={[
                    styles.addChip,
                    { backgroundColor: isSent ? '#fff' : T2.brand, borderColor: isSent ? T2.border : T2.brand },
                  ]}
                >
                  <Text style={{ fontSize: 12, fontWeight: '700', color: isSent ? T2.textMute : '#fff', letterSpacing: -0.2 }}>
                    {isSent ? '신청함' : '+ 메이트 추가'}
                  </Text>
                </Pressable>
              </View>
            );
          })}
        </View>
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  scroll: { paddingHorizontal: 20, paddingTop: 4, paddingBottom: 40 },
  search: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 9,
    marginBottom: 8,
    paddingVertical: 12,
    paddingHorizontal: 14,
    borderRadius: 12,
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: T2.border,
  },
  searchText: { fontSize: 14, color: T2.textMute, letterSpacing: -0.2 },

  label: { fontSize: 11, fontWeight: '700', color: T2.textMute, letterSpacing: 0.6, marginTop: 4, marginBottom: 12 },

  card: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 13,
    padding: 14,
    backgroundColor: '#fff',
    borderRadius: 16,
    borderWidth: 1,
    borderColor: T2.border,
  },
  nameRow: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  name: { flexShrink: 1, fontSize: 15, fontWeight: '800', color: T2.text, letterSpacing: -0.3 },
  togetherBadge: { backgroundColor: T2.brandSoft, paddingHorizontal: 6, paddingVertical: 2, borderRadius: 5 },
  togetherText: { fontSize: 10, fontWeight: '700', color: T2.brand },
  meta: { fontSize: 12, color: T2.textMute, marginTop: 4 },
  mutual: { fontSize: 11, color: T2.brand, fontWeight: '600', marginTop: 6, letterSpacing: -0.2 },

  nowRow: { flexDirection: 'row', alignItems: 'center', gap: 5, marginTop: 5 },
  nowDot: { width: 6, height: 6, borderRadius: 3, backgroundColor: C.open },
  nowText: { fontSize: 12, fontWeight: '700', color: C.open, letterSpacing: -0.2 },
  nowPlace: { flexShrink: 1, fontSize: 12, color: T2.textMute, letterSpacing: -0.2 },

  tagRow: { flexDirection: 'row', gap: 5, marginTop: 8 },
  tag: { backgroundColor: T2.bg, borderWidth: 1, borderColor: T2.border, paddingHorizontal: 7, paddingVertical: 2, borderRadius: 6 },
  tagText: { fontSize: 11, fontWeight: '600', color: T2.textSub },

  mateChip: {
    alignSelf: 'flex-start',
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    paddingHorizontal: 11,
    paddingVertical: 7,
    borderRadius: 9,
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: T2.border,
  },
  mateChipText: { fontSize: 12, fontWeight: '700', color: T2.textSub, letterSpacing: -0.2 },
  addChip: { alignSelf: 'flex-start', paddingHorizontal: 12, paddingVertical: 7, borderRadius: 9, borderWidth: 1 },
});
