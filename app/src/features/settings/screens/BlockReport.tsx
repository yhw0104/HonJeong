// BlockReport — 차단 / 신고 관리 (원본: screens/BlockReport.jsx)
// 더보기 '차단 / 신고 관리'에서 진입. 차단 목록 / 신고 내역 탭.
import React, { useState } from 'react';
import { View, Text, Pressable, ScrollView, StyleSheet, Alert } from 'react-native';
import { Screen, MoreHeader, Avatar } from '@/shared/components';
import { T2, C } from '@/shared/theme';
import type { RootStackScreenProps } from '@/navigation/types';
import { useBlockedUsers, useMyReports, useUnblockUser } from '@/features/safety/queries';
import { reasonLabel, reportStatusLabel, formatDotDate } from '@/features/safety/reportCopy';

export function BlockReportScreen({ navigation }: RootStackScreenProps<'BlockReport'>) {
  const [tab, setTab] = useState<'block' | 'report'>('block');
  const blocks = useBlockedUsers();
  const reports = useMyReports();
  const unblock = useUnblockUser();

  const TABS = [
    { key: 'block' as const, label: '차단 목록', count: blocks.data?.length ?? 0 },
    { key: 'report' as const, label: '신고 내역', count: reports.data?.length ?? 0 },
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
              {blocks.isError
                ? '잠시 후 다시 시도해주세요'
                : '차단한 메이트는 서로의 프로필·혼밥 현황을 볼 수 없고, 같이 먹기 신청도 보낼 수 없어요.'}
            </Text>
            {blocks.isSuccess && (blocks.data?.length ?? 0) === 0 ? (
              <Text style={styles.intro}>차단한 사용자가 없어요</Text>
            ) : null}
            <View style={{ gap: 10 }}>
              {(blocks.data ?? []).map((b) => (
                <View key={b.userId} style={styles.card}>
                  <Avatar uri={b.profileImageUrl} size={44} />
                  <View style={{ flex: 1, minWidth: 0 }}>
                    <Text style={styles.name}>{b.nickname ?? '알 수 없음'}</Text>
                    <Text style={styles.meta}>{formatDotDate(b.createdAt)} 차단</Text>
                  </View>
                  <Pressable
                    style={styles.unblockBtn}
                    disabled={unblock.isPending}
                    onPress={() =>
                      Alert.alert('차단 해제', `${b.nickname ?? '이 사용자'}님을 차단 해제할까요?`, [
                        { text: '취소', style: 'cancel' },
                        {
                          text: '해제',
                          style: 'destructive',
                          onPress: () =>
                            unblock.mutate(b.userId, {
                              onError: () => Alert.alert('차단 해제 실패', '잠시 후 다시 시도해주세요.'),
                            }),
                        },
                      ])
                    }
                  >
                    <Text style={styles.unblockText}>차단 해제</Text>
                  </Pressable>
                </View>
              ))}
            </View>
          </>
        ) : (
          <>
            <Text style={styles.intro}>
              {reports.isError
                ? '잠시 후 다시 시도해주세요'
                : '신고는 운영팀이 확인 후 조치하며, 처리 결과를 알림으로 알려드려요.'}
            </Text>
            {reports.isSuccess && (reports.data?.length ?? 0) === 0 ? (
              <Text style={styles.intro}>신고 내역이 없어요</Text>
            ) : null}
            <View style={{ gap: 10 }}>
              {(reports.data ?? []).map((r) => {
                const done = r.status === 'RESOLVED';
                return (
                  <View key={r.id} style={styles.reportCard}>
                    <View style={styles.reportHead}>
                      <Text style={styles.reportTarget}>{r.targetNickname}</Text>
                      <View style={[styles.statusPill, { backgroundColor: done ? 'rgba(34,166,90,0.1)' : T2.brandSoft }]}>
                        <Text style={{ fontSize: 11, fontWeight: '700', color: done ? C.openDark : T2.brand, letterSpacing: -0.2 }}>{reportStatusLabel(r.status)}</Text>
                      </View>
                    </View>
                    <View style={styles.reportMeta}>
                      <Text style={styles.reportReason}>사유 · {reasonLabel(r.reasonCode)}</Text>
                      <Text style={styles.metaDot}>·</Text>
                      <Text style={styles.reportDate}>{formatDotDate(r.createdAt)}</Text>
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
