// More — 더보기 / 마이페이지 (원본: screens/More.jsx)
// 프로필 카드 + 3섹션 메뉴 + 로그아웃. 메뉴 대상 화면은 아직 미변환이라 onPress no-op.
import React, { useCallback } from 'react';
import { View, Text, Pressable, ScrollView, StyleSheet, Alert } from 'react-native';
import { Screen, Avatar, Icon } from '@/shared/components';
import type { IconName } from '@/shared/components';
import { T2 } from '@/shared/theme';
import { useAuth } from '@/shared/auth/AuthContext';
import type { MainTabScreenProps } from '@/navigation/types';
import { useFocusEffect } from '@react-navigation/native';
import { useMyProfile, useActivitySummary } from '@/features/users/queries';
import { useReceivedRequests } from '@/features/meal/queries';
import { diningStyleLabel } from '@/shared/format';
import { BellButton } from '@/features/notifications/BellButton';

type MenuRoute =
  | 'ReceivedRequests'
  | 'DiningHistory'
  | 'MyReviews'
  | 'ChallengeBadges'
  | 'BlockReport'
  | 'NotificationSettings'
  | 'Notices'
  | 'Support';
type MenuItem = { l: string; d?: string; icon: IconName; accent?: boolean; badge?: string; route?: MenuRoute };
type Section = { title: string; items: MenuItem[] };

const SECTIONS: Section[] = [
  {
    title: '나의 혼밥',
    items: [
      { l: '내 혼밥 기록', icon: 'book', route: 'DiningHistory' },
      { l: '내가 쓴 리뷰', icon: 'note', route: 'MyReviews' },
      { l: '혼밥 챌린지 · 뱃지', d: '획득 7 / 20', icon: 'badge', accent: true, route: 'ChallengeBadges' },
    ],
  },
  {
    title: '메이트',
    items: [
      // 받은 신청 건수(d·badge)는 렌더 시 실데이터로 채운다(아래 menuDetail·menuBadge).
      { l: '같이 먹기 신청', icon: 'mate', route: 'ReceivedRequests' },
      { l: '차단 / 신고 관리', icon: 'shield', route: 'BlockReport' },
    ],
  },
  {
    title: '설정',
    items: [
      { l: '알림 설정', icon: 'bell', route: 'NotificationSettings' },
      { l: '공지사항', icon: 'note', route: 'Notices' },
      { l: '고객센터 · 문의', icon: 'help', route: 'Support' },
    ],
  },
];


export function MoreScreen({ navigation }: MainTabScreenProps<'More'>) {
  const { signOut } = useAuth();
  const { data: profile } = useMyProfile();
  const activity = useActivitySummary();
  const summary = activity.data;
  // 상대가 내 신청을 수락하거나 메이트를 해제하면 내 앱엔 트리거가 없으므로, 더보기 재진입 시 통계를 새로고침한다.
  useFocusEffect(useCallback(() => { activity.refetch(); }, [activity.refetch]));
  // 받은 같이먹기 신청(PENDING) — 같이먹기 화면과 같은 쿼리라 캐시 공유·라이브 폴링으로 갱신된다.
  const received = useReceivedRequests('PENDING');
  const pendingCount = received.data?.length;
  const num = (n?: number) => (n === undefined ? '–' : String(n));
  const stats: { n: string; l: string; route: 'DiningHistory' | 'Mates' }[] = [
    { n: num(summary?.checkInCount), l: '혼밥', route: 'DiningHistory' },
    { n: num(summary?.togetherCount), l: '같이먹음', route: 'DiningHistory' },
    { n: num(summary?.mateCount), l: '메이트', route: 'Mates' },
  ];
  // '내 혼밥 기록'·'받은 같이 먹기 신청' detail은 실데이터, 나머지 메뉴는 정적 d 유지.
  const menuDetail = (it: MenuItem) => {
    if (it.route === 'DiningHistory') return `${num(summary?.checkInCount)}회 · 일기 ${num(summary?.reviewCount)}편`;
    if (it.route === 'ReceivedRequests') return pendingCount === undefined ? undefined : `${pendingCount}건`;
    return it.d;
  };
  // 뱃지(빨간 카운트)는 받은 신청이 1건 이상일 때만 노출.
  const menuBadge = (it: MenuItem) =>
    it.route === 'ReceivedRequests' ? (pendingCount ? String(pendingCount) : undefined) : it.badge;

  const onLogout = () => {
    Alert.alert('로그아웃', '로그아웃하시겠어요?', [
      { text: '취소', style: 'cancel' },
      { text: '로그아웃', style: 'destructive', onPress: () => signOut() },
    ]);
  };

  return (
    <Screen bg={T2.bg} edges={['top']}>
      <ScrollView contentContainerStyle={{ paddingBottom: 16 }}>
        {/* 헤더 — 타이틀 + 알림 종 */}
        <View style={[styles.header, { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' }]}>
          <Text style={styles.h1}>더보기</Text>
          <BellButton />
        </View>

        {/* 프로필 카드 */}
        <View style={styles.cardWrap}>
          <View style={styles.card}>
            <Pressable style={styles.cardTop} onPress={() => navigation.navigate('MyProfile')}>
              <Avatar uri={profile?.profileImageUrl} bg={T2.bg} size={52} />
              <View style={{ flex: 1, minWidth: 0 }}>
                <Text style={styles.profileName}>{profile?.nickname ?? '혼밥러'}</Text>
                {/* 내 동네 표기는 제거(설정 기능 없음) — 식사 성향으로 대체, 미설정이면 혼밥 횟수만. */}
                <Text style={styles.profileMeta}>
                  {[`혼밥 ${num(summary?.checkInCount)}회`, diningStyleLabel(profile?.diningStyle)]
                    .filter(Boolean)
                    .join(' · ')}
                </Text>
              </View>
              <Icon name="chevronRight" size={18} color={T2.textMute} />
            </Pressable>

            <View style={styles.statsRow}>
              {stats.map((s, k) => (
                <Pressable
                  key={s.l}
                  style={[styles.statCell, k > 0 && styles.statDivider]}
                  onPress={() => navigation.navigate(s.route)}
                >
                  <Text style={styles.statNum}>{s.n}</Text>
                  <Text style={styles.statLabel}>{s.l}</Text>
                </Pressable>
              ))}
            </View>
          </View>
        </View>

        {/* 섹션들 */}
        {SECTIONS.map((sec, si) => (
          <View key={sec.title} style={{ marginTop: si === 0 ? 4 : 18 }}>
            <Text style={styles.sectionTitle}>{sec.title}</Text>
            <View style={styles.sectionBody}>
              {sec.items.map((it, ii) => (
                <Pressable
                  key={it.l}
                  style={[styles.menuRow, ii < sec.items.length - 1 && styles.menuDivider]}
                  // 미변환 대상은 no-op, 변환된 화면만 route로 연결
                  onPress={it.route ? () => navigation.navigate(it.route!) : undefined}
                >
                  <View style={[styles.menuIcon, { backgroundColor: it.accent ? T2.brandSoft : T2.bg }]}>
                    <Icon name={it.icon} size={20} color={it.accent ? T2.brand : T2.text} />
                  </View>
                  <Text style={styles.menuLabel}>{it.l}</Text>
                  {menuBadge(it) ? (
                    <View style={styles.menuBadge}>
                      <Text style={styles.menuBadgeText}>{menuBadge(it)}</Text>
                    </View>
                  ) : null}
                  {menuDetail(it) ? <Text style={styles.menuDetail}>{menuDetail(it)}</Text> : null}
                  <Icon name="chevronRight" size={16} color={T2.textMute} />
                </Pressable>
              ))}
            </View>
          </View>
        ))}

        {/* 로그아웃 */}
        <View style={styles.footer}>
          <Pressable onPress={onLogout}>
            <Text style={styles.footerText}>로그아웃</Text>
          </Pressable>
          <Text style={styles.footerText}>버전 1.0.0</Text>
        </View>
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  header: { paddingHorizontal: 20, paddingTop: 12, paddingBottom: 8 },
  h1: { fontSize: 28, fontWeight: '800', color: T2.text, letterSpacing: -1 },

  cardWrap: { paddingHorizontal: 20, paddingTop: 12, paddingBottom: 20 },
  card: { padding: 18, backgroundColor: '#fff', borderRadius: 18, borderWidth: 1, borderColor: T2.border },
  cardTop: { flexDirection: 'row', alignItems: 'center', gap: 14 },
  profileName: { fontSize: 17, fontWeight: '800', color: T2.text, letterSpacing: -0.4 },
  profileMeta: { fontSize: 12, color: T2.textSub, marginTop: 4, letterSpacing: -0.2 },

  statsRow: { flexDirection: 'row', marginTop: 16, paddingTop: 16, borderTopWidth: 1, borderTopColor: T2.border },
  statCell: { flex: 1, alignItems: 'center' },
  statDivider: { borderLeftWidth: 1, borderLeftColor: T2.border },
  statNum: { fontSize: 18, fontWeight: '800', color: T2.text, letterSpacing: -0.5 },
  statLabel: { fontSize: 11, color: T2.textMute, marginTop: 3, letterSpacing: -0.2 },

  sectionTitle: { fontSize: 11, fontWeight: '700', color: T2.textMute, letterSpacing: 0.6, paddingHorizontal: 20, paddingBottom: 8 },
  sectionBody: { backgroundColor: '#fff', borderTopWidth: 1, borderBottomWidth: 1, borderColor: T2.border },
  menuRow: { flexDirection: 'row', alignItems: 'center', gap: 14, paddingVertical: 15, paddingHorizontal: 20 },
  menuDivider: { borderBottomWidth: 1, borderBottomColor: T2.border },
  menuIcon: { width: 36, height: 36, borderRadius: 10, alignItems: 'center', justifyContent: 'center' },
  menuLabel: { flex: 1, fontSize: 15, fontWeight: '600', color: T2.text, letterSpacing: -0.3 },
  menuBadge: {
    minWidth: 18,
    height: 18,
    borderRadius: 9,
    paddingHorizontal: 5,
    backgroundColor: T2.brand,
    alignItems: 'center',
    justifyContent: 'center',
  },
  menuBadgeText: { fontSize: 11, fontWeight: '800', color: '#fff' },
  menuDetail: { fontSize: 13, color: T2.textMute, letterSpacing: -0.2 },

  footer: { flexDirection: 'row', gap: 18, paddingHorizontal: 20, paddingTop: 24, paddingBottom: 32 },
  footerText: { fontSize: 13, color: T2.textMute, fontWeight: '600', letterSpacing: -0.2 },
});
