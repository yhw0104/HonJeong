// MyProfile — 내 프로필 (원본: screens/MyProfile.jsx)
// 더보기 프로필 카드에서 진입. 편집→ProfileEdit, 획득한 뱃지 전체보기→ChallengeBadges.
import React from 'react';
import { View, Text, Pressable, ScrollView, StyleSheet, Dimensions } from 'react-native';
import { Screen, Avatar, Icon, StateView } from '@/shared/components';
import { T2 } from '@/shared/theme';
import { useMyProfile, useActivitySummary } from '@/features/users/queries';
import { useBadges } from '@/features/record/queries';
import { toBadgeViews } from '@/features/record/badges';
import { BadgeMedal } from '@/features/record/BadgeMedal';
import { diningStyleLabel, ageGenderLabel } from '@/shared/format';
import type { RootStackScreenProps } from '@/navigation/types';

// 획득 뱃지 strip — 한 줄에 4개가 폭을 꽉 채우도록 메달 크기 계산(오른쪽 슬랙 제거).
const BADGE_COLS = 4;
const BADGE_GAP = 10;
const BADGE_MEDAL = Math.floor((Dimensions.get('window').width - 40 - BADGE_GAP * (BADGE_COLS - 1)) / BADGE_COLS);

export function MyProfileScreen({ navigation }: RootStackScreenProps<'MyProfile'>) {
  const { data: profile, isLoading, isError, refetch } = useMyProfile();
  const { data: summary } = useActivitySummary();
  const { data: badgeData } = useBadges();
  // 획득한 뱃지 전부 — 뱃지 화면과 같은 순서(BADGE_DEFS 정의 순, 획득한 것만).
  const earnedBadges = toBadgeViews(badgeData ?? []).filter((v) => v.earned);
  const num = (n?: number) => (n === undefined ? '–' : String(n));
  const stats = [
    { n: num(summary?.checkInCount), l: '혼밥', go: () => navigation.navigate('DiningHistory') },
    { n: num(summary?.togetherCount), l: '같이먹음', go: () => navigation.navigate('DiningHistory') },
    { n: num(summary?.mateCount), l: '메이트', go: () => navigation.navigate('Mates') },
  ];
  const foods = profile?.favoriteFoods ?? [];
  // 이름 아래 서브라인: "20대 여성 · 도란도란 대화하며" — 있는 것만 이어 붙인다.
  const subLine = [ageGenderLabel(profile?.ageGroup, profile?.gender), diningStyleLabel(profile?.diningStyle)]
    .filter(Boolean)
    .join(' · ');
  const styleLabel =
    profile?.diningStyle === 'QUIET'
      ? { title: '조용히 각자', sub: '편하게, 말 없이 먹어도 좋아요' }
      : { title: '도란도란 대화하며', sub: '가볍게 이야기 나누는 게 좋아요' };

  return (
    <Screen bg={T2.bg} edges={['top']}>
      {/* 헤더 */}
      <View style={styles.header}>
        <Pressable onPress={() => navigation.goBack()} hitSlop={10} style={styles.headerBtn}>
          <Icon name="chevronLeft" size={22} color={T2.text} />
        </Pressable>
        <Pressable onPress={() => navigation.navigate('ProfileEdit')} hitSlop={10}>
          <Text style={styles.edit}>편집</Text>
        </Pressable>
      </View>

      {isLoading ? (
        <StateView kind="loading" />
      ) : isError ? (
        <StateView kind="error" onRetry={() => refetch()} />
      ) : (
        <ScrollView contentContainerStyle={styles.scroll}>
        {/* 프로필 헤더 */}
        <View style={styles.profile}>
          <Avatar uri={profile?.profileImageUrl} bg={T2.bg} size={84} />
          <Text style={styles.name}>{profile?.nickname ?? '혼밥러'}</Text>
          {subLine ? <Text style={styles.sub}>{subLine}</Text> : null}
          {!!profile?.introduction && <Text style={styles.bio}>"{profile.introduction}"</Text>}
        </View>

        {/* 통계 */}
        <View style={styles.statsCard}>
          {stats.map((s, i) => (
            <Pressable key={s.l} style={[styles.statCell, i > 0 && styles.statDivider]} onPress={s.go}>
              <Text style={styles.statNum}>{s.n}</Text>
              <Text style={styles.statLabel}>{s.l}</Text>
            </Pressable>
          ))}
        </View>

        {/* 좋아하는 음식 */}
        <View style={{ marginTop: 28 }}>
          <Text style={styles.sectionLabel}>좋아하는 음식</Text>
          {foods.length === 0 ? (
            <Text style={styles.foodEmpty}>아직 선택한 음식이 없어요</Text>
          ) : (
            <View style={styles.chipWrap}>
              {foods.map((f) => (
                <View key={f} style={styles.foodChip}>
                  <Text style={styles.foodText}>{f}</Text>
                </View>
              ))}
            </View>
          )}
        </View>

        {/* 같이 먹을 때 */}
        <View style={{ marginTop: 28 }}>
          <Text style={styles.sectionLabel}>같이 먹을 때</Text>
          <View style={styles.styleCard}>
            <Icon name="chat" size={22} color={T2.brand} />
            <View>
              <Text style={styles.styleTitle}>{styleLabel.title}</Text>
              <Text style={styles.styleSub}>{styleLabel.sub}</Text>
            </View>
          </View>
        </View>

        {/* 최근 획득 뱃지 */}
        <View style={{ marginTop: 28 }}>
          <View style={styles.badgeHead}>
            <Text style={[styles.sectionLabel, { marginBottom: 0 }]}>획득한 뱃지</Text>
            <Pressable onPress={() => navigation.navigate('ChallengeBadges')} hitSlop={8}>
              <Text style={styles.viewAll}>전체보기</Text>
            </Pressable>
          </View>
          <View style={styles.badgeRow}>
            {earnedBadges.length === 0 ? (
              <Text style={styles.foodEmpty}>아직 뱃지가 없어요 · 혼밥으로 시작해요</Text>
            ) : (
              earnedBadges.map((b) => (
                <BadgeMedal key={b.key} icon={b.icon} tier={b.tier} tierNum={b.tierNum} earned={b.earned} size={BADGE_MEDAL} />
              ))
            )}
          </View>
        </View>
        </ScrollView>
      )}
    </Screen>
  );
}

const styles = StyleSheet.create({
  header: { height: 52, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingHorizontal: 12 },
  headerBtn: { width: 40, height: 40, borderRadius: 20, alignItems: 'center', justifyContent: 'center' },
  edit: { fontSize: 14, fontWeight: '700', color: T2.brand, letterSpacing: -0.2, paddingHorizontal: 8 },

  scroll: { paddingHorizontal: 20, paddingTop: 8, paddingBottom: 40 },

  profile: { alignItems: 'center', paddingTop: 8 },
  name: { fontSize: 22, fontWeight: '800', color: T2.text, letterSpacing: -0.6, marginTop: 14 },
  sub: { fontSize: 13, color: T2.textMute, marginTop: 5, letterSpacing: -0.2 },
  bio: { fontSize: 14, color: T2.textSub, marginTop: 14, lineHeight: 22, letterSpacing: -0.3, textAlign: 'center', maxWidth: 280 },

  statsCard: { flexDirection: 'row', marginTop: 24, paddingVertical: 18, backgroundColor: '#fff', borderRadius: 18, borderWidth: 1, borderColor: T2.border },
  statCell: { flex: 1, alignItems: 'center' },
  statDivider: { borderLeftWidth: 1, borderLeftColor: T2.border },
  statNum: { fontSize: 22, fontWeight: '800', color: T2.text, letterSpacing: -0.6 },
  statLabel: { fontSize: 11, color: T2.textMute, marginTop: 4 },

  sectionLabel: { fontSize: 11, fontWeight: '700', color: T2.textMute, letterSpacing: 0.6, marginBottom: 12 },
  chipWrap: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  foodChip: { paddingHorizontal: 14, paddingVertical: 9, borderRadius: 999, backgroundColor: '#fff', borderWidth: 1, borderColor: T2.borderStrong },
  foodText: { fontSize: 13, fontWeight: '600', color: T2.text, letterSpacing: -0.2 },
  foodEmpty: { fontSize: 13, color: T2.textMute, letterSpacing: -0.2 },

  styleCard: { flexDirection: 'row', alignItems: 'center', gap: 12, padding: 16, borderRadius: 14, backgroundColor: T2.text },
  styleTitle: { fontSize: 15, fontWeight: '700', color: '#fff', letterSpacing: -0.3 },
  styleSub: { fontSize: 12, color: 'rgba(255,255,255,0.6)', marginTop: 2 },

  badgeHead: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 },
  viewAll: { fontSize: 12, fontWeight: '700', color: T2.brand },
  badgeRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 10, alignItems: 'center' },
});
