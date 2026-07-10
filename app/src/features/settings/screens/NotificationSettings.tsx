// NotificationSettings — 알림 설정 (더보기 '알림 설정'에서 진입)
// 실제 발행되는 알림(같이먹기·메이트)과 미래용 수신 동의(공지·이벤트혜택) 4토글을 서버에 저장.
// 토글 off = 백엔드가 그 종류 알림을 발행 시점에 생성하지 않음(같이먹기·메이트). 공지·이벤트는 해당 기능 생길 때 적용.
import React from 'react';
import { View, Text, ScrollView, StyleSheet, ActivityIndicator } from 'react-native';
import { Screen, MoreHeader, Toggle } from '@/shared/components';
import { T2 } from '@/shared/theme';
import type { RootStackScreenProps } from '@/navigation/types';
import { useNotificationSettings, useUpdateNotificationSettings } from '@/features/notifications/queries';
import type { NotificationSettings } from '@/features/notifications/api';

type Item = { key: keyof NotificationSettings; title: string; desc: string };
type Group = { title: string; items: Item[] };

const GROUPS: Group[] = [
  {
    title: '활동',
    items: [
      { key: 'meal', title: '같이 먹기 알림', desc: '같이 먹기 신청·수락 알림' },
      { key: 'mate', title: '메이트 알림', desc: '메이트 신청·수락 알림' },
    ],
  },
  {
    title: '소식',
    items: [
      { key: 'notice', title: '공지사항', desc: '서비스 주요 공지' },
      { key: 'marketing', title: '이벤트 · 혜택', desc: '할인·이벤트 소식' },
    ],
  },
];

export function NotificationSettingsScreen({ navigation }: RootStackScreenProps<'NotificationSettings'>) {
  const { data, isLoading, isError } = useNotificationSettings();
  const update = useUpdateNotificationSettings();

  const toggle = (key: keyof NotificationSettings) => {
    if (!data) return;
    update.mutate({ ...data, [key]: !data[key] });
  };

  return (
    <Screen bg={T2.bg} edges={['top']}>
      <MoreHeader title="알림 설정" onBack={() => navigation.goBack()} />

      {isLoading ? (
        <View style={styles.center}>
          <ActivityIndicator color={T2.textMute} />
        </View>
      ) : isError || !data ? (
        <View style={styles.center}>
          <Text style={styles.errorText}>설정을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.</Text>
        </View>
      ) : (
        <ScrollView contentContainerStyle={{ paddingBottom: 40 }}>
          {GROUPS.map((g) => (
            <View key={g.title} style={{ marginTop: 18 }}>
              <Text style={styles.groupLabel}>{g.title}</Text>
              <View style={styles.block}>
                {g.items.map((it, i) => (
                  <View key={it.key} style={[styles.row, i < g.items.length - 1 && styles.rowDivider]}>
                    <View style={{ flex: 1, minWidth: 0 }}>
                      <Text style={styles.rowTitle}>{it.title}</Text>
                      <Text style={styles.rowDesc}>{it.desc}</Text>
                    </View>
                    <Toggle value={data[it.key]} onValueChange={() => toggle(it.key)} />
                  </View>
                ))}
              </View>
            </View>
          ))}

          <Text style={styles.footnote}>
            끈 알림은 알림함에도 쌓이지 않아요. 공지·이벤트 알림은 해당 기능이 준비되면 이 설정을 따릅니다.
          </Text>
        </ScrollView>
      )}
    </Screen>
  );
}

const styles = StyleSheet.create({
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', padding: 24 },
  errorText: { fontSize: 14, color: T2.textMute, textAlign: 'center', lineHeight: 21 },

  groupLabel: { fontSize: 11, fontWeight: '700', color: T2.textMute, letterSpacing: 0.6, paddingHorizontal: 20, paddingBottom: 8 },
  block: { backgroundColor: '#fff', borderTopWidth: 1, borderBottomWidth: 1, borderColor: T2.border },
  row: { flexDirection: 'row', alignItems: 'center', gap: 14, paddingVertical: 15, paddingHorizontal: 20 },
  rowDivider: { borderBottomWidth: 1, borderBottomColor: T2.border },
  rowTitle: { fontSize: 15, fontWeight: '600', color: T2.text, letterSpacing: -0.3 },
  rowDesc: { fontSize: 12, color: T2.textMute, marginTop: 3, letterSpacing: -0.2 },

  footnote: { paddingHorizontal: 20, paddingTop: 18, fontSize: 12, color: T2.textMute, lineHeight: 19, letterSpacing: -0.2 },
});
