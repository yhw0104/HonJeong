// ChallengeBadges — 혼밥 챌린지 · 뱃지 (원본: screens/ChallengeBadges.jsx)
// 더보기 '챌린지·뱃지' 또는 프로필 '뱃지' 스탯에서 진입. 뱃지 탭 시 획득 방법 바텀시트.
import React, { useState } from 'react';
import { View, Text, Pressable, ScrollView, StyleSheet, Dimensions } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Screen, MoreHeader, StateView } from '@/shared/components';
import { T2 } from '@/shared/theme';
import { useBadges } from '@/features/record/queries';
import { toBadgeViews, earnedCount, BADGE_DEFS, type BadgeView } from '@/features/record/badges';
import { BadgeMedal } from '@/features/record/BadgeMedal';
import type { RootStackScreenProps } from '@/navigation/types';

// 한 줄에 3칸: (화면폭 − 좌우 패딩 40 − 갭 2개 24) / 3. floor로 살짝 줄여 반올림 줄바꿈(2칸 보임) 방지.
const GRID_W = Math.floor((Dimensions.get('window').width - 40 - 24) / 3);

export function ChallengeBadgesScreen({ navigation }: RootStackScreenProps<'ChallengeBadges'>) {
  const { data, isLoading, isError, refetch } = useBadges();
  const statuses = data ?? [];
  const views = toBadgeViews(statuses);
  const earned = earnedCount(statuses);
  const [selected, setSelected] = useState<BadgeView | null>(null);

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
          <Text style={styles.badgeHeadCount}>획득 {earned} / {BADGE_DEFS.length}</Text>
        </View>

        {/* 뱃지 그리드 — 로딩/에러 정직 처리(에러를 가짜 획득으로 위장하지 않음) */}
        {isLoading ? (
          <StateView kind="loading" />
        ) : isError ? (
          <StateView kind="error" onRetry={() => refetch()} />
        ) : (
          <View style={styles.grid}>
            {views.map((b) => (
              <Pressable
                key={b.key}
                onPress={() => setSelected(b)}
                accessibilityRole="button"
                accessibilityLabel={`${b.name} 뱃지, ${b.earned ? '획득함' : '아직 획득 전'}`}
                style={[
                  styles.badgeCell,
                  { width: GRID_W, backgroundColor: b.earned ? '#fff' : T2.bg, borderColor: b.earned ? T2.border : 'transparent' },
                ]}
              >
                <BadgeMedal icon={b.icon} tier={b.tier} tierNum={b.tierNum} earned={b.earned} size={Math.round(GRID_W * 0.56)} />
                <Text style={[styles.badgeName, { color: b.earned ? T2.text : T2.textMute }]}>{b.name}</Text>
              </Pressable>
            ))}
          </View>
        )}
      </ScrollView>

      {/* 뱃지 상세 — 획득 방법 바텀시트 */}
      {selected && <BadgeDetailSheet view={selected} onClose={() => setSelected(null)} />}
    </Screen>
  );
}

/** 획득 시각(ISO)을 'N월 N일 획득'으로. 파싱 실패 시 null. */
function formatEarnedDate(iso: string): string | null {
  const d = new Date(iso);
  if (isNaN(d.getTime())) return null;
  return `${d.getMonth() + 1}월 ${d.getDate()}일 획득`;
}

/** 뱃지 탭 시 획득 방법·상태를 보여주는 하단 시트(DirectionsSheet 톤). */
function BadgeDetailSheet({ view, onClose }: { view: BadgeView; onClose: () => void }) {
  const insets = useSafeAreaInsets();
  const earnedDate = view.earnedAt ? formatEarnedDate(view.earnedAt) : null;
  return (
    <>
      <Pressable style={styles.scrim} onPress={onClose} accessibilityRole="button" accessibilityLabel="닫기" />
      <View style={[styles.sheet, { paddingBottom: insets.bottom + 24 }]}>
        <Pressable style={styles.close} onPress={onClose} hitSlop={8} accessibilityRole="button" accessibilityLabel="닫기">
          <Text style={styles.closeX}>×</Text>
        </Pressable>
        <View style={styles.handle} />
        <View style={{ marginTop: 4 }}>
          <BadgeMedal icon={view.icon} tier={view.tier} tierNum={view.tierNum} earned={view.earned} size={96} />
        </View>
        <Text style={styles.sheetName}>{view.name}</Text>
        <Text style={styles.sheetHowLabel}>이렇게 획득해요</Text>
        <Text style={styles.sheetHow}>{view.how}</Text>
        <View style={[styles.sheetStatus, { backgroundColor: view.earned ? T2.brandSoft : T2.bg }]}>
          <Text style={[styles.sheetStatusText, { color: view.earned ? T2.brand : T2.textMute }]}>
            {view.earned ? `✓ ${earnedDate ?? '획득함'}` : '아직 획득 전'}
          </Text>
        </View>
      </View>
    </>
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
  badgeName: { fontSize: 12, fontWeight: '700', marginTop: 10, letterSpacing: -0.2, textAlign: 'center' },

  // 상세 바텀시트
  scrim: { position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, zIndex: 90, backgroundColor: 'rgba(10,10,10,0.4)' },
  sheet: {
    position: 'absolute', left: 0, right: 0, bottom: 0, zIndex: 91,
    backgroundColor: '#fff', borderTopLeftRadius: 24, borderTopRightRadius: 24,
    paddingTop: 10, paddingHorizontal: 24, alignItems: 'center',
    shadowColor: '#000', shadowOffset: { width: 0, height: -8 }, shadowOpacity: 0.18, shadowRadius: 30, elevation: 12,
  },
  close: { position: 'absolute', top: 10, right: 12, width: 34, height: 34, alignItems: 'center', justifyContent: 'center', zIndex: 2 },
  closeX: { fontSize: 24, color: T2.textMute, lineHeight: 26 },
  handle: { width: 36, height: 4, borderRadius: 2, backgroundColor: '#E5E5E5', alignSelf: 'center', marginBottom: 18 },
  sheetName: { fontSize: 20, fontWeight: '800', color: T2.text, letterSpacing: -0.5, marginTop: 16 },
  sheetHowLabel: { fontSize: 11, fontWeight: '700', color: T2.textMute, letterSpacing: 0.6, marginTop: 20 },
  sheetHow: { fontSize: 15, fontWeight: '600', color: T2.textSub, letterSpacing: -0.3, marginTop: 8, textAlign: 'center', lineHeight: 22 },
  sheetStatus: { marginTop: 22, paddingHorizontal: 16, paddingVertical: 10, borderRadius: 999 },
  sheetStatusText: { fontSize: 13, fontWeight: '800', letterSpacing: -0.2 },
});
