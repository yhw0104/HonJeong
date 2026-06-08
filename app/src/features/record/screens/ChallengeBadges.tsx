// ChallengeBadges — 혼밥 챌린지 · 뱃지 (원본: screens/ChallengeBadges.jsx)
// 더보기 '챌린지·뱃지' 또는 프로필 '뱃지' 스탯에서 진입.
import React from 'react';
import { View, Text, ScrollView, StyleSheet, Dimensions } from 'react-native';
import { Screen, MoreHeader } from '@/shared/components';
import { T2 } from '@/shared/theme';
import type { RootStackScreenProps } from '@/navigation/types';

const BADGES = [
  { e: '🌱', n: '첫 혼밥', got: true },
  { e: '🍚', n: '혼밥 10회', got: true },
  { e: '🔥', n: '3일 연속', got: true },
  { e: '🍜', n: '한식 마스터', got: true },
  { e: '🤝', n: '첫 같이 먹기', got: true },
  { e: '📷', n: '일기 10편', got: true },
  { e: '🌙', n: '혼밥 디너', got: true },
  { e: '🏆', n: '혼밥 50회', got: false },
  { e: '🗺️', n: '동네 정복', got: false },
  { e: '⭐', n: '리뷰 30개', got: false },
  { e: '🎂', n: '생일 혼밥', got: false },
  { e: '🥇', n: '레벨 5', got: false },
];

const GRID_W = (Dimensions.get('window').width - 40 - 24) / 3;

export function ChallengeBadgesScreen({ navigation }: RootStackScreenProps<'ChallengeBadges'>) {
  return (
    <Screen bg={T2.bg} edges={['top']}>
      <MoreHeader title="혼밥 챌린지 · 뱃지" onBack={() => navigation.goBack()} />

      <ScrollView contentContainerStyle={styles.scroll}>
        {/* 진행 중 챌린지 */}
        <View style={styles.challenge}>
          <Text style={styles.challengeEyebrow}>이번 주 챌린지</Text>
          <Text style={styles.challengeTitle}>새로운 동네에서 혼밥하기</Text>
          <View style={{ marginTop: 16 }}>
            <View style={styles.progressLabelRow}>
              <Text style={styles.progressLabel}>진행률</Text>
              <Text style={styles.progressLabel}>
                <Text style={{ color: T2.brand, fontWeight: '800' }}>2</Text> / 3 곳
              </Text>
            </View>
            <View style={styles.progressTrack}>
              <View style={styles.progressFill} />
            </View>
          </View>
        </View>

        {/* 뱃지 요약 */}
        <View style={styles.badgeHead}>
          <Text style={styles.badgeHeadTitle}>내 뱃지</Text>
          <Text style={styles.badgeHeadCount}>7 / 12 획득</Text>
        </View>

        {/* 뱃지 그리드 */}
        <View style={styles.grid}>
          {BADGES.map((b) => (
            <View
              key={b.n}
              style={[
                styles.badgeCell,
                { width: GRID_W, backgroundColor: b.got ? '#fff' : T2.bg, borderColor: b.got ? T2.border : 'transparent', opacity: b.got ? 1 : 0.5 },
              ]}
            >
              <View style={[styles.badgeIcon, { backgroundColor: b.got ? T2.brandSoft : 'rgba(0,0,0,0.04)' }]}>
                <Text style={{ fontSize: 26 }}>{b.got ? b.e : '🔒'}</Text>
              </View>
              <Text style={[styles.badgeName, { color: b.got ? T2.text : T2.textMute }]}>{b.n}</Text>
            </View>
          ))}
        </View>
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  scroll: { paddingHorizontal: 20, paddingTop: 8, paddingBottom: 40 },

  challenge: { padding: 20, borderRadius: 18, backgroundColor: T2.text, marginTop: 4 },
  challengeEyebrow: { fontSize: 11, fontWeight: '700', color: 'rgba(255,255,255,0.6)', letterSpacing: 0.6 },
  challengeTitle: { fontSize: 19, fontWeight: '800', color: '#fff', letterSpacing: -0.5, marginTop: 8 },
  progressLabelRow: { flexDirection: 'row', justifyContent: 'space-between', marginBottom: 7 },
  progressLabel: { fontSize: 12, fontWeight: '600', color: 'rgba(255,255,255,0.7)' },
  progressTrack: { height: 7, borderRadius: 4, backgroundColor: 'rgba(255,255,255,0.15)', overflow: 'hidden' },
  progressFill: { width: '66%', height: '100%', backgroundColor: T2.brand, borderRadius: 4 },

  badgeHead: { flexDirection: 'row', alignItems: 'baseline', gap: 8, marginTop: 28, marginBottom: 14 },
  badgeHeadTitle: { fontSize: 15, fontWeight: '800', color: T2.text, letterSpacing: -0.4 },
  badgeHeadCount: { fontSize: 13, fontWeight: '700', color: T2.textMute },

  grid: { flexDirection: 'row', flexWrap: 'wrap', gap: 12 },
  badgeCell: { paddingTop: 18, paddingBottom: 14, paddingHorizontal: 8, borderRadius: 16, borderWidth: 1, alignItems: 'center' },
  badgeIcon: { width: 52, height: 52, borderRadius: 26, alignItems: 'center', justifyContent: 'center' },
  badgeName: { fontSize: 12, fontWeight: '700', marginTop: 10, letterSpacing: -0.2, textAlign: 'center' },
});
