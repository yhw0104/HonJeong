// NotificationSettings — 알림 설정 (원본: screens/NotificationSettings.jsx)
// 더보기 '알림 설정'에서 진입. 마스터 스위치 + 그룹별 토글 + 방해 금지 시간.
import React, { useState } from 'react';
import { View, Text, ScrollView, StyleSheet } from 'react-native';
import { Screen, MoreHeader, Toggle } from '@/shared/components';
import { T2 } from '@/shared/theme';
import type { RootStackScreenProps } from '@/navigation/types';

type Item = { key: string; title: string; desc?: string };
type Group = { title: string; items: Item[] };

const GROUPS: Group[] = [
  {
    title: '활동',
    items: [
      { key: 'mealReq', title: '같이 먹기 신청', desc: '새 신청·수락·거절 알림' },
      { key: 'reviewReact', title: '혼밥 인증 · 리뷰 반응', desc: '좋아요·댓글이 달리면' },
      { key: 'challenge', title: '챌린지 · 뱃지', desc: '달성 현황과 새 뱃지' },
    ],
  },
  {
    title: '메이트',
    items: [
      { key: 'mateReq', title: '메이트 신청', desc: '누군가 메이트로 추가하면' },
      { key: 'mateStart', title: '메이트 혼밥 시작', desc: '내 메이트가 근처에서 혼밥을 시작하면' },
    ],
  },
  {
    title: '마케팅 · 정보',
    items: [
      { key: 'event', title: '이벤트 · 혜택', desc: '할인·프로모션 소식' },
      { key: 'notice', title: '공지사항', desc: '서비스 주요 변경 안내' },
    ],
  },
];

const INITIAL: Record<string, boolean> = {
  push: true,
  mealReq: true,
  reviewReact: true,
  challenge: false,
  mateReq: true,
  mateStart: true,
  event: false,
  notice: true,
  dnd: true,
};

export function NotificationSettingsScreen({ navigation }: RootStackScreenProps<'NotificationSettings'>) {
  const [on, setOn] = useState<Record<string, boolean>>(INITIAL);
  const toggle = (k: string) => setOn((p) => ({ ...p, [k]: !p[k] }));

  return (
    <Screen bg={T2.bg} edges={['top']}>
      <MoreHeader title="알림 설정" onBack={() => navigation.goBack()} />

      <ScrollView contentContainerStyle={{ paddingBottom: 40 }}>
        {/* 마스터 스위치 */}
        <View style={{ paddingHorizontal: 20, paddingTop: 4 }}>
          <View style={styles.master}>
            <View style={{ flex: 1 }}>
              <Text style={styles.masterTitle}>푸시 알림</Text>
              <Text style={styles.masterSub}>끄면 아래 모든 알림이 꺼져요</Text>
            </View>
            <Toggle value={on.push} onValueChange={() => toggle('push')} />
          </View>
        </View>

        {GROUPS.map((g) => (
          <View key={g.title} style={{ marginTop: 18 }}>
            <Text style={styles.groupLabel}>{g.title}</Text>
            <View style={styles.block}>
              {g.items.map((it, i) => (
                <View key={it.key} style={[styles.row, i < g.items.length - 1 && styles.rowDivider]}>
                  <View style={{ flex: 1, minWidth: 0 }}>
                    <Text style={styles.rowTitle}>{it.title}</Text>
                    {it.desc ? <Text style={styles.rowDesc}>{it.desc}</Text> : null}
                  </View>
                  <Toggle value={on[it.key]} onValueChange={() => toggle(it.key)} />
                </View>
              ))}
            </View>
          </View>
        ))}

        {/* 방해 금지 시간 */}
        <View style={{ marginTop: 18 }}>
          <Text style={styles.groupLabel}>방해 금지 시간</Text>
          <View style={styles.block}>
            <View style={[styles.row, styles.rowDivider]}>
              <View style={{ flex: 1, minWidth: 0 }}>
                <Text style={styles.rowTitle}>야간 방해 금지</Text>
                <Text style={styles.rowDesc}>설정한 시간에는 알림을 받지 않아요</Text>
              </View>
              <Toggle value={on.dnd} onValueChange={() => toggle('dnd')} />
            </View>
            <View style={styles.row}>
              <Text style={[styles.rowTitle, { flex: 1 }]}>시간</Text>
              <View style={styles.timeRow}>
                <View style={styles.timePill}>
                  <Text style={styles.timeText}>22:00</Text>
                </View>
                <Text style={styles.timeDash}>–</Text>
                <View style={styles.timePill}>
                  <Text style={styles.timeText}>08:00</Text>
                </View>
              </View>
            </View>
          </View>
        </View>

        <Text style={styles.footnote}>
          기기 설정에서 알림이 꺼져 있으면 위 설정과 무관하게 알림이 오지 않을 수 있어요.
        </Text>
      </ScrollView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  master: { flexDirection: 'row', alignItems: 'center', gap: 14, padding: 18, backgroundColor: T2.text, borderRadius: 16 },
  masterTitle: { fontSize: 16, fontWeight: '800', color: '#fff', letterSpacing: -0.3 },
  masterSub: { fontSize: 12, color: 'rgba(255,255,255,0.6)', marginTop: 3 },

  groupLabel: { fontSize: 11, fontWeight: '700', color: T2.textMute, letterSpacing: 0.6, paddingHorizontal: 20, paddingBottom: 8 },
  block: { backgroundColor: '#fff', borderTopWidth: 1, borderBottomWidth: 1, borderColor: T2.border },
  row: { flexDirection: 'row', alignItems: 'center', gap: 14, paddingVertical: 15, paddingHorizontal: 20 },
  rowDivider: { borderBottomWidth: 1, borderBottomColor: T2.border },
  rowTitle: { fontSize: 15, fontWeight: '600', color: T2.text, letterSpacing: -0.3 },
  rowDesc: { fontSize: 12, color: T2.textMute, marginTop: 3, letterSpacing: -0.2 },

  timeRow: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  timePill: { paddingHorizontal: 12, paddingVertical: 6, backgroundColor: T2.bg, borderRadius: 9, borderWidth: 1, borderColor: T2.border },
  timeText: { fontSize: 14, fontWeight: '700', color: T2.text },
  timeDash: { color: T2.textMute },

  footnote: { paddingHorizontal: 20, paddingTop: 18, fontSize: 12, color: T2.textMute, lineHeight: 19, letterSpacing: -0.2 },
});
