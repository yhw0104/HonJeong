// BlockReport — 차단 / 신고 관리 (원본: screens/BlockReport.jsx)
// 더보기 '차단 / 신고 관리'에서 진입. 차단 목록 / 신고 내역 탭.
import React, { useState } from 'react';
import { View, Text, Pressable, ScrollView, StyleSheet } from 'react-native';
import { Screen, MoreHeader, EmojiCircle } from '@/shared/components';
import { T2, C } from '@/shared/theme';
import type { RootStackScreenProps } from '@/navigation/types';

const BLOCKED = [
  { name: '소란한식객', emo: '🍔', date: '2026.05.28' },
  { name: '늦참러', emo: '🥡', date: '2026.05.12' },
];
const REPORTS = [
  { target: '익명 메이트', reason: '부적절한 메시지', date: '2026.05.30', status: '처리 완료' },
  { target: '게시물 리뷰', reason: '광고 · 스팸', date: '2026.05.20', status: '검토 중' },
];

export function BlockReportScreen({ navigation }: RootStackScreenProps<'BlockReport'>) {
  const [tab, setTab] = useState<'block' | 'report'>('block');
  const TABS = [
    { key: 'block' as const, label: '차단 목록', count: BLOCKED.length },
    { key: 'report' as const, label: '신고 내역', count: REPORTS.length },
  ];

  return (
    <Screen bg={T2.bg} edges={['top']}>
      <MoreHeader title="차단 / 신고 관리" onBack={() => navigation.goBack()} />

      {/* 탭 */}
      <View style={styles.tabRow}>
        {TABS.map((s) => {
          const on = tab === s.key;
          return (
            <Pressable key={s.key} onPress={() => setTab(s.key)} style={styles.tab}>
              <Text style={[styles.tabLabel, { color: on ? T2.text : T2.textMute, fontWeight: on ? '800' : '600' }]}>{s.label}</Text>
              <Text style={[styles.tabCount, { color: on ? T2.brand : T2.textMute }]}>{s.count}</Text>
              {on ? <View style={styles.tabUnderline} /> : null}
            </Pressable>
          );
        })}
      </View>
      <View style={styles.divider} />

      <ScrollView contentContainerStyle={styles.scroll}>
        {tab === 'block' ? (
          <>
            <Text style={styles.intro}>
              차단한 메이트는 서로의 프로필·혼밥 현황을 볼 수 없고, 같이 먹기 신청도 보낼 수 없어요.
            </Text>
            <View style={{ gap: 10 }}>
              {BLOCKED.map((b) => (
                <View key={b.name} style={styles.card}>
                  <EmojiCircle emoji={b.emo} size={44} dimmed />
                  <View style={{ flex: 1, minWidth: 0 }}>
                    <Text style={styles.name}>{b.name}</Text>
                    <Text style={styles.meta}>{b.date} 차단</Text>
                  </View>
                  <Pressable style={styles.unblockBtn}>
                    <Text style={styles.unblockText}>차단 해제</Text>
                  </Pressable>
                </View>
              ))}
            </View>
          </>
        ) : (
          <>
            <Text style={styles.intro}>신고는 운영팀이 확인 후 조치하며, 처리 결과를 알림으로 알려드려요.</Text>
            <View style={{ gap: 10 }}>
              {REPORTS.map((r) => {
                const done = r.status === '처리 완료';
                return (
                  <View key={r.target} style={styles.reportCard}>
                    <View style={styles.reportHead}>
                      <Text style={styles.reportTarget}>{r.target}</Text>
                      <View style={[styles.statusPill, { backgroundColor: done ? 'rgba(34,166,90,0.1)' : T2.brandSoft }]}>
                        <Text style={{ fontSize: 11, fontWeight: '700', color: done ? C.openDark : T2.brand, letterSpacing: -0.2 }}>{r.status}</Text>
                      </View>
                    </View>
                    <View style={styles.reportMeta}>
                      <Text style={styles.reportReason}>사유 · {r.reason}</Text>
                      <Text style={styles.metaDot}>·</Text>
                      <Text style={styles.reportDate}>{r.date}</Text>
                    </View>
                  </View>
                );
              })}
            </View>
          </>
        )}
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  tabRow: { flexDirection: 'row', gap: 24, paddingHorizontal: 20 },
  tab: { flexDirection: 'row', alignItems: 'center', gap: 6, paddingBottom: 12 },
  tabLabel: { fontSize: 15, letterSpacing: -0.3 },
  tabCount: { fontSize: 12, fontWeight: '700' },
  tabUnderline: { position: 'absolute', left: 0, right: 0, bottom: 0, height: 2, backgroundColor: T2.brand },
  divider: { height: 1, backgroundColor: T2.border },

  scroll: { paddingHorizontal: 20, paddingVertical: 16, paddingBottom: 40 },
  intro: { fontSize: 12, color: T2.textMute, lineHeight: 19, letterSpacing: -0.2, marginBottom: 14 },

  card: { flexDirection: 'row', alignItems: 'center', gap: 13, padding: 14, backgroundColor: '#fff', borderRadius: 14, borderWidth: 1, borderColor: T2.border },
  name: { fontSize: 15, fontWeight: '700', color: T2.text, letterSpacing: -0.3 },
  meta: { fontSize: 12, color: T2.textMute, marginTop: 3 },
  unblockBtn: { paddingHorizontal: 14, paddingVertical: 8, borderRadius: 9, backgroundColor: '#fff', borderWidth: 1, borderColor: T2.borderStrong },
  unblockText: { fontSize: 13, fontWeight: '700', color: T2.text, letterSpacing: -0.2 },

  reportCard: { padding: 16, backgroundColor: '#fff', borderRadius: 14, borderWidth: 1, borderColor: T2.border },
  reportHead: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  reportTarget: { flex: 1, fontSize: 15, fontWeight: '700', color: T2.text, letterSpacing: -0.3 },
  statusPill: { paddingHorizontal: 9, paddingVertical: 4, borderRadius: 999 },
  reportMeta: { flexDirection: 'row', alignItems: 'center', gap: 8, marginTop: 10 },
  reportReason: { fontSize: 13, color: T2.textSub, letterSpacing: -0.2 },
  metaDot: { color: T2.textMute },
  reportDate: { fontSize: 13, color: T2.textMute, letterSpacing: -0.2 },
});
