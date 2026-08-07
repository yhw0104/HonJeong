// PushPermission — 푸시 권한 사전 안내. OS 팝업을 띄우기 전에 왜 필요한지 먼저 설명한다.
//
// iOS는 한 번 거부당하면 앱에서 다시 물어볼 수 없다 — 그래서 '나중에 할래요'는
// OS 팝업을 아예 띄우지 않고 그냥 넘어간다. 한 번뿐인 기회를 아끼기 위해서다.
// 나중에 마음이 바뀌면 더보기 → 알림 설정에서 다시 시도할 수 있다.
import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { CTAButton, Screen } from '@/shared/components';
import { T2 } from '@/shared/theme';
import type { RootStackScreenProps } from '@/navigation/types';
import { registerPushToken, requestPushPermission } from '@/shared/push';
import { markPushPromptSeen } from '@/shared/push/prompt';

export function PushPermissionScreen({ navigation }: RootStackScreenProps<'PushPermission'>) {
  const [busy, setBusy] = React.useState(false);

  // 수락·거절과 무관하게 '안내를 띄웠다'는 사실 자체를 남긴다.
  // 그래야 거절한 사람에게 앱을 켤 때마다 다시 들이대지 않는다.
  const close = React.useCallback(() => {
    void markPushPromptSeen();
    navigation.goBack();
  }, [navigation]);

  const allow = React.useCallback(async () => {
    if (busy) return;
    setBusy(true);
    try {
      // 허용했을 때만 토큰을 등록한다. 거부여도 화면은 그냥 닫는다 —
      // 여기서 붙잡아 봐야 iOS는 다시 물어볼 방법이 없다.
      if (await requestPushPermission()) {
        await registerPushToken();
      }
    } finally {
      setBusy(false);
      close();
    }
  }, [busy, close]);

  return (
    <Screen bg={T2.bg}>
      <View style={styles.body}>
        <Text style={styles.title}>알림을{'\n'}받으시겠어요?</Text>
        <Text style={styles.desc}>
          같이 먹기 신청이 오거나 상대가 메시지를 보내면 바로 알려드릴게요.{'\n'}
          앱을 켜두지 않아도 놓치지 않아요.
        </Text>
        <View style={styles.list}>
          <Text style={styles.item}>· 같이 먹기 신청과 수락</Text>
          <Text style={styles.item}>· 메이트 신청과 수락</Text>
          <Text style={styles.item}>· 대화 새 메시지</Text>
        </View>
        <Text style={styles.note}>대화별 알림은 대화 목록에서 하나씩 끌 수 있어요.</Text>
      </View>
      <View style={styles.actions}>
        <CTAButton label="알림 받기" onPress={allow} disabled={busy} />
        <Text style={styles.later} onPress={close} suppressHighlighting>
          나중에 할래요
        </Text>
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  body: { flex: 1, paddingHorizontal: 28, paddingTop: 48 },
  title: { fontSize: 30, fontWeight: '800', color: T2.text, letterSpacing: -1, lineHeight: 35 },
  desc: { fontSize: 14, color: T2.textSub, marginTop: 14, lineHeight: 21, letterSpacing: -0.3 },
  list: { gap: 6, marginTop: 24 },
  item: { fontSize: 14, color: T2.text, letterSpacing: -0.3 },
  note: { fontSize: 12, color: T2.textMute, marginTop: 20, letterSpacing: -0.2 },
  actions: { paddingHorizontal: 24, paddingTop: 12, paddingBottom: 12 },
  later: {
    marginTop: 8,
    fontSize: 14,
    fontWeight: '600',
    color: T2.textMute,
    textAlign: 'center',
    paddingVertical: 12,
    letterSpacing: -0.3,
  },
});
