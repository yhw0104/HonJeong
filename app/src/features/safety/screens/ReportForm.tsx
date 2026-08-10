// ReportForm — 신고 접수 모달. MateProfile 케밥 · (추후) 리뷰 케밥에서 진입.
// 사유(라디오, 필수) + 상세(선택) 입력 → 접수. USER 신고 성공 시 차단을 이어서 제안한다.
import React, { useState } from 'react';
import { View, Text, TextInput, Pressable, ScrollView, StyleSheet, Alert } from 'react-native';
import { Screen, MoreHeader, CTAButton } from '@/shared/components';
import { T2 } from '@/shared/theme';
import type { RootStackScreenProps } from '@/navigation/types';
import { useCreateReport, useBlockUser } from '@/features/safety/queries';
import { REPORT_REASONS } from '@/features/safety/reportCopy';

export function ReportFormScreen({ navigation, route }: RootStackScreenProps<'ReportForm'>) {
  const { targetType, targetId, targetNickname } = route.params;
  const [reason, setReason] = useState<string | null>(null);
  const [detail, setDetail] = useState('');
  const create = useCreateReport();
  const block = useBlockUser();

  const onSubmit = () => {
    if (!reason) return;
    create.mutate(
      { targetType, targetId, reasonCode: reason, detail: detail.trim() || undefined },
      {
        onSuccess: () => {
          if (targetType === 'USER') {
            // 합의된 플로우: 접수 완료 후 차단 제안
            Alert.alert('신고가 접수됐어요', `${targetNickname}님을 차단할까요?`, [
              { text: '나중에', style: 'cancel', onPress: () => navigation.goBack() },
              {
                text: '차단하기',
                style: 'destructive',
                onPress: () =>
                  block.mutate(targetId, {
                    onSuccess: () => navigation.goBack(),
                    onError: () => Alert.alert('차단 실패', '잠시 후 다시 시도해주세요.'),
                  }),
              },
            ]);
          } else {
            Alert.alert('신고가 접수됐어요', '운영 확인 후 조치할게요.', [
              { text: '확인', onPress: () => navigation.goBack() },
            ]);
          }
        },
        onError: () => Alert.alert('접수 실패', '잠시 후 다시 시도해주세요.'),
      },
    );
  };

  return (
    <Screen bg={T2.bg} edges={['top']}>
      <MoreHeader title="신고하기" onBack={() => navigation.goBack()} />

      {/* automaticallyAdjustKeyboardInsets — 아래쪽 입력칸이 키보드에 가리지 않게(DiningLogWrite와 같은 이유). */}
      <ScrollView
        contentContainerStyle={styles.scroll}
        keyboardShouldPersistTaps="handled"
        keyboardDismissMode="on-drag"
        automaticallyAdjustKeyboardInsets
      >
        <Text style={styles.target}>
          {targetType === 'REVIEW' ? `${targetNickname}님의 리뷰` : `${targetNickname}님`}
        </Text>

        <Text style={styles.label}>신고 사유</Text>
        {REPORT_REASONS.map((r) => {
          const on = reason === r.code;
          return (
            <Pressable
              key={r.code}
              onPress={() => setReason(r.code)}
              style={[
                styles.radioRow,
                { backgroundColor: on ? T2.text : '#fff', borderColor: on ? T2.text : T2.border },
              ]}
            >
              <View
                style={[
                  styles.radio,
                  { borderColor: on ? T2.brand : T2.borderStrong, backgroundColor: on ? T2.brand : 'transparent' },
                ]}
              >
                {on ? <View style={styles.radioInner} /> : null}
              </View>
              <Text style={{ fontSize: 15, fontWeight: '700', color: on ? '#fff' : T2.text, letterSpacing: -0.3 }}>
                {r.label}
              </Text>
            </Pressable>
          );
        })}

        <View style={{ marginTop: 24 }}>
          <Text style={styles.label}>상세 내용 (선택)</Text>
          <TextInput
            style={styles.detailInput}
            value={detail}
            onChangeText={setDetail}
            multiline
            maxLength={500}
            placeholder="자세한 내용을 적어주시면 확인에 도움이 돼요 (선택)"
            placeholderTextColor={T2.textMute}
          />
        </View>
      </ScrollView>

      <View style={styles.ctaWrap}>
        <CTAButton
          label="신고 접수"
          disabled={!reason || create.isPending || block.isPending}
          onPress={onSubmit}
        />
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  scroll: { paddingHorizontal: 20, paddingTop: 8, paddingBottom: 24 },

  target: { fontSize: 16, fontWeight: '800', color: T2.text, letterSpacing: -0.4, marginBottom: 24 },

  label: { fontSize: 12, fontWeight: '700', color: T2.textMute, letterSpacing: 0.5, marginBottom: 10 },

  radioRow: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 14, paddingHorizontal: 16, borderRadius: 12, marginBottom: 8, borderWidth: 1 },
  radio: { width: 18, height: 18, borderRadius: 9, borderWidth: 2, alignItems: 'center', justifyContent: 'center' },
  radioInner: { width: 6, height: 6, borderRadius: 3, backgroundColor: T2.text },

  detailInput: {
    marginTop: 2,
    paddingVertical: 14,
    paddingHorizontal: 16,
    borderRadius: 12,
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: T2.border,
    fontSize: 14,
    color: T2.text,
    lineHeight: 21,
    letterSpacing: -0.3,
    minHeight: 100,
    textAlignVertical: 'top',
  },

  ctaWrap: { paddingHorizontal: 24, paddingTop: 12, paddingBottom: 24, backgroundColor: T2.bg },
});
