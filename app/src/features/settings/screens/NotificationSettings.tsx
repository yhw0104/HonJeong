// NotificationSettings — 알림 설정 (더보기 '알림 설정'에서 진입)
// 실제 발행되는 알림(같이먹기·메이트)과 미래용 수신 동의(공지·이벤트혜택) 4토글을 서버에 저장.
// 토글 off = 백엔드가 그 종류 알림을 발행 시점에 생성하지 않음(같이먹기·메이트). 공지·이벤트는 해당 기능 생길 때 적용.
import React from 'react';
import { View, Text, ScrollView, StyleSheet, ActivityIndicator, Pressable, Alert, Linking } from 'react-native';
import { Screen, MoreHeader, Toggle } from '@/shared/components';
import { T2 } from '@/shared/theme';
import type { RootStackScreenProps } from '@/navigation/types';
import { useNotificationSettings, useUpdateNotificationSettings } from '@/features/notifications/queries';
import type { NotificationSettings } from '@/features/notifications/api';
import { hasPushPermission, registerPushToken, requestPushPermission } from '@/shared/push';

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

  // OS 푸시 권한 상태. 아래 토글은 '서버가 알림을 만들지' 여부일 뿐이라,
  // OS 권한이 없으면 토글이 다 켜져 있어도 휴대폰에는 아무것도 안 뜬다.
  // null = 확인 중(깜빡임을 막으려고 확인 전에는 카드를 그리지 않는다).
  const [pushGranted, setPushGranted] = React.useState<boolean | null>(null);
  React.useEffect(() => {
    void hasPushPermission().then(setPushGranted);
  }, []);

  // 권한이 없을 때의 유일한 복구 경로다. iOS는 한 번 거부되면 앱에서 팝업을 못 띄우므로,
  // 요청이 또 거부로 끝나면(=이미 거부한 상태) OS 설정으로 보내는 수밖에 없다.
  const openPushPrompt = React.useCallback(async () => {
    if (await requestPushPermission()) {
      await registerPushToken();
      setPushGranted(true);
    } else {
      Alert.alert('알림이 꺼져 있어요', '휴대폰 설정 > 혼정 > 알림에서 켤 수 있어요.', [
        { text: '확인' },
        { text: '설정 열기', onPress: () => void Linking.openSettings() },
      ]);
    }
  }, []);

  return (
    <Screen bg={T2.bg} edges={['top']}>
      <MoreHeader title="알림 설정" onBack={() => navigation.goBack()} />

      {/* 이 카드가 없으면 화면이 거짓말을 한다 — 토글은 전부 켜져 있는데 OS 권한이 없어
          아무 알림도 안 오는 상태가 되고, 사용자는 그 이유를 알 방법이 없다. */}
      {pushGranted === false && (
        <Pressable style={styles.pushOffCard} onPress={openPushPrompt}>
          <Text style={styles.pushOffTitle}>휴대폰 알림이 꺼져 있어요</Text>
          <Text style={styles.pushOffDesc}>
            아래 설정을 켜도 휴대폰에는 알림이 뜨지 않아요. 눌러서 켜기
          </Text>
        </Pressable>
      )}

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

  // 경고가 아니라 안내라서 빨강 대신 브랜드 틴트를 쓴다(ProfileSetup의 인증 배지와 같은 결).
  pushOffCard: {
    marginTop: 16,
    marginHorizontal: 20,
    paddingVertical: 14,
    paddingHorizontal: 16,
    borderRadius: 12,
    backgroundColor: T2.brandSoft,
    borderWidth: 1,
    borderColor: T2.brand,
  },
  pushOffTitle: { fontSize: 14, fontWeight: '700', color: T2.brand, letterSpacing: -0.3 },
  pushOffDesc: { fontSize: 12, color: T2.textSub, marginTop: 4, lineHeight: 18, letterSpacing: -0.2 },

  groupLabel: { fontSize: 11, fontWeight: '700', color: T2.textMute, letterSpacing: 0.6, paddingHorizontal: 20, paddingBottom: 8 },
  block: { backgroundColor: '#fff', borderTopWidth: 1, borderBottomWidth: 1, borderColor: T2.border },
  row: { flexDirection: 'row', alignItems: 'center', gap: 14, paddingVertical: 15, paddingHorizontal: 20 },
  rowDivider: { borderBottomWidth: 1, borderBottomColor: T2.border },
  rowTitle: { fontSize: 15, fontWeight: '600', color: T2.text, letterSpacing: -0.3 },
  rowDesc: { fontSize: 12, color: T2.textMute, marginTop: 3, letterSpacing: -0.2 },

  footnote: { paddingHorizontal: 20, paddingTop: 18, fontSize: 12, color: T2.textMute, lineHeight: 19, letterSpacing: -0.2 },
});
