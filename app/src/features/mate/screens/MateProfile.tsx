// MateProfile — 메이트 프로필(다른 사람) (원본: screens/MateProfile.jsx)
// Mates 목록에서 진입. 프로필/통계/선호/공개 즐겨찾기 + 하단 CTA(메이트/같이먹기).
import React from 'react';
import { View, Text, Pressable, ScrollView, StyleSheet } from 'react-native';
import { Screen, EmojiCircle, Icon } from '@/shared/components';
import { T2, C } from '@/shared/theme';
import type { RootStackScreenProps } from '@/navigation/types';

const FOODS = ['한식', '면 요리', '디저트'];
const STATS = [
  { n: '32', l: '혼밥' },
  { n: '2', l: '함께 먹음' },
  { n: '9', l: '뱃지' },
];
const PUBLIC_GROUPS = [
  { name: '혼밥 입문 코스', emo: '🍲', count: 5, note: '부담 없는 첫 혼밥' },
  { name: '연남 국수 지도', emo: '🍜', count: 3, note: '면 요리 모음' },
];

export function MateProfileScreen({ navigation, route }: RootStackScreenProps<'MateProfile'>) {
  const name = route.params?.name ?? '점심혼밥러';

  return (
    <Screen bg={T2.bg} edges={['top']}>
      {/* 헤더 */}
      <View style={styles.header}>
        <Pressable onPress={() => navigation.goBack()} hitSlop={10} style={styles.headerBtn}>
          <Icon name="chevronLeft" size={22} color={T2.text} />
        </Pressable>
        <View style={styles.headerBtn}>
          <View style={styles.dotsRow}>
            {[0, 1, 2].map((d) => (
              <View key={d} style={styles.dot} />
            ))}
          </View>
        </View>
      </View>

      <ScrollView contentContainerStyle={styles.scroll}>
        {/* 프로필 헤더 */}
        <View style={styles.profile}>
          <EmojiCircle emoji="🍙" size={84} online />
          <View style={styles.nameRow}>
            <Text style={styles.name}>{name}</Text>
            <View style={styles.togetherBadge}>
              <Text style={styles.togetherText}>같이 2회</Text>
            </View>
          </View>
          <View style={styles.nowPill}>
            <View style={styles.nowDot} />
            <Text style={styles.nowText}>지금 혼밥 중</Text>
            <Text style={styles.nowPlace}>· 큰순두부 연남점</Text>
          </View>
          <Text style={styles.sub}>연남동 · 혼밥 1년차</Text>
          <Text style={styles.bio}>
            "점심은 거의 혼밥! 순두부랑 국수 좋아해요.{'\n'}편하게 같이 드실 분 환영이에요."
          </Text>
        </View>

        {/* 통계 */}
        <View style={styles.statsCard}>
          {STATS.map((s, i) => (
            <View key={s.l} style={[styles.statCell, i > 0 && styles.statDivider]}>
              <Text style={[styles.statNum, { color: i === 1 ? T2.brand : T2.text }]}>{s.n}</Text>
              <Text style={styles.statLabel}>{s.l}</Text>
            </View>
          ))}
        </View>

        {/* 좋아하는 음식 */}
        <View style={{ marginTop: 28 }}>
          <Text style={styles.sectionLabel}>좋아하는 음식</Text>
          <View style={styles.chipWrap}>
            {FOODS.map((f) => (
              <View key={f} style={styles.foodChip}>
                <Text style={styles.foodText}>{f}</Text>
              </View>
            ))}
          </View>
        </View>

        {/* 같이 먹을 때 */}
        <View style={{ marginTop: 28 }}>
          <Text style={styles.sectionLabel}>같이 먹을 때</Text>
          <View style={styles.styleCard}>
            <Text style={{ fontSize: 22 }}>💬</Text>
            <View>
              <Text style={styles.styleTitle}>도란도란 대화하며</Text>
              <Text style={styles.styleSub}>가볍게 이야기 나누는 게 좋아요</Text>
            </View>
          </View>
        </View>

        {/* 공개 즐겨찾기 */}
        <View style={{ marginTop: 28 }}>
          <View style={styles.pubLabelRow}>
            <Text style={styles.sectionLabel}>공개 즐겨찾기</Text>
            <Text style={styles.pubCount}>{PUBLIC_GROUPS.length}</Text>
          </View>
          <View style={{ gap: 10 }}>
            {PUBLIC_GROUPS.map((g) => (
              <Pressable key={g.name} style={styles.groupCard}>
                <View style={styles.groupThumb}>
                  <Text style={{ fontSize: 22 }}>{g.emo}</Text>
                </View>
                <View style={{ flex: 1, minWidth: 0 }}>
                  <View style={styles.groupNameRow}>
                    <Text style={styles.groupName} numberOfLines={1}>
                      {g.name}
                    </Text>
                    <View style={styles.pubBadge}>
                      <Text style={styles.pubBadgeText}>공개</Text>
                    </View>
                  </View>
                  <View style={styles.groupMetaRow}>
                    <Text style={styles.groupCountText}>{g.count}곳</Text>
                    <Text style={styles.groupDot}>·</Text>
                    <Text style={styles.groupNote} numberOfLines={1}>
                      {g.note}
                    </Text>
                  </View>
                </View>
                <Icon name="chevronRight" size={18} color={T2.textMute} />
              </Pressable>
            ))}
          </View>
        </View>
      </ScrollView>

      {/* 하단 CTA */}
      <View style={styles.ctaBar}>
        <Pressable style={styles.mateBtn}>
          <Text style={{ color: T2.brand, fontSize: 16 }}>✓</Text>
          <Text style={styles.mateBtnText}>메이트</Text>
        </Pressable>
        <Pressable style={styles.mealBtn} onPress={() => navigation.navigate('MealRequest', { name })}>
          <Text style={styles.mealBtnText}>같이 먹기 신청</Text>
        </Pressable>
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  header: { height: 52, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 12 },
  headerBtn: { width: 40, height: 40, borderRadius: 20, alignItems: 'center', justifyContent: 'center' },
  dotsRow: { flexDirection: 'row', gap: 3 },
  dot: { width: 3.5, height: 3.5, borderRadius: 2, backgroundColor: T2.text },

  scroll: { paddingHorizontal: 20, paddingTop: 8, paddingBottom: 24 },

  profile: { alignItems: 'center', paddingTop: 8 },
  nameRow: { flexDirection: 'row', alignItems: 'center', gap: 6, marginTop: 14 },
  name: { fontSize: 22, fontWeight: '800', color: T2.text, letterSpacing: -0.6 },
  togetherBadge: { backgroundColor: T2.brandSoft, paddingHorizontal: 6, paddingVertical: 2, borderRadius: 5 },
  togetherText: { fontSize: 10, fontWeight: '700', color: T2.brand },
  nowPill: { flexDirection: 'row', alignItems: 'center', gap: 6, marginTop: 10, paddingVertical: 6, paddingHorizontal: 12, borderRadius: 999, backgroundColor: 'rgba(34,166,90,0.1)' },
  nowDot: { width: 7, height: 7, borderRadius: 4, backgroundColor: C.open },
  nowText: { fontSize: 12, fontWeight: '700', color: C.openDark, letterSpacing: -0.2 },
  nowPlace: { fontSize: 12, color: T2.textSub, letterSpacing: -0.2 },
  sub: { fontSize: 13, color: T2.textMute, marginTop: 10, letterSpacing: -0.2 },
  bio: { fontSize: 14, color: T2.textSub, marginTop: 14, lineHeight: 22, letterSpacing: -0.3, textAlign: 'center', maxWidth: 280 },

  statsCard: { flexDirection: 'row', marginTop: 24, paddingVertical: 18, backgroundColor: '#fff', borderRadius: 18, borderWidth: 1, borderColor: T2.border },
  statCell: { flex: 1, alignItems: 'center' },
  statDivider: { borderLeftWidth: 1, borderLeftColor: T2.border },
  statNum: { fontSize: 22, fontWeight: '800', letterSpacing: -0.6 },
  statLabel: { fontSize: 11, color: T2.textMute, marginTop: 4 },

  sectionLabel: { fontSize: 11, fontWeight: '700', color: T2.textMute, letterSpacing: 0.6, marginBottom: 12 },
  chipWrap: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  foodChip: { paddingHorizontal: 14, paddingVertical: 9, borderRadius: 999, backgroundColor: '#fff', borderWidth: 1, borderColor: T2.borderStrong },
  foodText: { fontSize: 13, fontWeight: '600', color: T2.text, letterSpacing: -0.2 },

  styleCard: { flexDirection: 'row', alignItems: 'center', gap: 12, padding: 16, borderRadius: 14, backgroundColor: T2.text },
  styleTitle: { fontSize: 15, fontWeight: '700', color: '#fff', letterSpacing: -0.3 },
  styleSub: { fontSize: 12, color: 'rgba(255,255,255,0.6)', marginTop: 2 },

  pubLabelRow: { flexDirection: 'row', alignItems: 'baseline', gap: 6 },
  pubCount: { fontSize: 11, fontWeight: '700', color: T2.brand, marginBottom: 12 },
  groupCard: { flexDirection: 'row', alignItems: 'center', gap: 13, padding: 14, backgroundColor: '#fff', borderRadius: 16, borderWidth: 1, borderColor: T2.border },
  groupThumb: { width: 50, height: 50, borderRadius: 12, backgroundColor: T2.bg, borderWidth: 1, borderColor: T2.border, alignItems: 'center', justifyContent: 'center' },
  groupNameRow: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  groupName: { flexShrink: 1, fontSize: 15, fontWeight: '800', color: T2.text, letterSpacing: -0.3 },
  pubBadge: { flexDirection: 'row', alignItems: 'center', gap: 3, backgroundColor: T2.bg, borderWidth: 1, borderColor: T2.border, paddingHorizontal: 6, paddingVertical: 2, borderRadius: 5 },
  pubBadgeText: { fontSize: 10, fontWeight: '700', color: T2.textSub },
  groupMetaRow: { flexDirection: 'row', alignItems: 'center', gap: 7, marginTop: 4 },
  groupCountText: { fontSize: 12, fontWeight: '700', color: T2.text },
  groupDot: { fontSize: 12, color: T2.textMute },
  groupNote: { flexShrink: 1, fontSize: 12, color: T2.textSub },

  ctaBar: { flexDirection: 'row', gap: 10, paddingHorizontal: 16, paddingTop: 12, paddingBottom: 28, backgroundColor: '#fff', borderTopWidth: 1, borderTopColor: T2.border },
  mateBtn: { flexDirection: 'row', alignItems: 'center', gap: 6, paddingHorizontal: 18, paddingVertical: 16, borderRadius: 12, backgroundColor: T2.bg },
  mateBtnText: { fontSize: 14, fontWeight: '700', color: T2.textSub, letterSpacing: -0.3 },
  mealBtn: { flex: 1, paddingVertical: 16, borderRadius: 12, backgroundColor: T2.brand, alignItems: 'center' },
  mealBtnText: { fontSize: 15, fontWeight: '700', color: '#fff', letterSpacing: -0.3 },
});
