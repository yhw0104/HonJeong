// PhoneAuth — 휴대폰 번호 인증 01/03 (원본: screens/PhoneAuth.jsx)
// 목업의 가짜 <div> 입력을 실제 TextInput으로, 약관은 useState 토글로 구현.
import React, { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  Pressable,
  ScrollView,
  KeyboardAvoidingView,
  Platform,
  StyleSheet,
} from 'react-native';
import { Screen, StepProgress, CTAButton, Icon } from '@/shared/components';
import { T2 } from '@/shared/theme';
import type { RootStackScreenProps } from '@/navigation/types';

type Term = { key: string; label: string; req: boolean };
const TERMS: Term[] = [
  { key: 'tos', label: '서비스 이용약관', req: true },
  { key: 'privacy', label: '개인정보 처리방침', req: true },
  { key: 'location', label: '위치정보 이용 동의', req: true },
  { key: 'marketing', label: '마케팅 알림 수신', req: false },
];

export function PhoneAuthScreen({ navigation }: RootStackScreenProps<'PhoneAuth'>) {
  const [phone, setPhone] = useState('');
  const [checked, setChecked] = useState<Record<string, boolean>>({
    tos: true,
    privacy: true,
    location: true,
    marketing: false,
  });

  const allChecked = TERMS.every((t) => checked[t.key]);
  const requiredOk = TERMS.filter((t) => t.req).every((t) => checked[t.key]);
  const canProceed = requiredOk && phone.replace(/\D/g, '').length >= 8;

  const toggleAll = () => {
    const v = !allChecked;
    setChecked(Object.fromEntries(TERMS.map((t) => [t.key, v])));
  };
  const toggle = (k: string) => setChecked((p) => ({ ...p, [k]: !p[k] }));

  return (
    <Screen bg={T2.bg}>
      <KeyboardAvoidingView style={{ flex: 1 }} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        {/* 헤더 */}
        <View style={styles.header}>
          <Pressable onPress={() => navigation.goBack()} hitSlop={10}>
            <Text style={styles.backArrow}>←</Text>
          </Pressable>
        </View>

        <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
          <StepProgress step={1} />

          <Text style={styles.h1}>휴대폰 번호를{'\n'}입력해주세요</Text>
          <Text style={styles.lead}>
            인증 후 같은 동네 혼밥 친구를 안전하게 만나요. 번호는 공개되지 않습니다.
          </Text>

          {/* 번호 입력 */}
          <View style={{ marginTop: 40 }}>
            <Text style={styles.fieldLabel}>휴대폰 번호</Text>
            <View style={styles.inputRow}>
              <Text style={styles.cc}>+82</Text>
              <TextInput
                style={styles.input}
                value={phone}
                onChangeText={setPhone}
                placeholder="10 0000 0000"
                placeholderTextColor={T2.textMute}
                keyboardType="phone-pad"
                maxLength={13}
              />
            </View>
          </View>

          {/* 약관 */}
          <View style={{ marginTop: 32 }}>
            <Pressable style={styles.allRow} onPress={toggleAll}>
              <View style={[styles.allCheck, { backgroundColor: allChecked ? T2.text : '#fff', borderColor: allChecked ? T2.text : T2.borderStrong }]}>
                <Text style={styles.checkMark}>✓</Text>
              </View>
              <Text style={styles.allText}>약관에 모두 동의</Text>
            </Pressable>

            {TERMS.map((t) => {
              const on = checked[t.key];
              return (
                <Pressable key={t.key} style={styles.termRow} onPress={() => toggle(t.key)}>
                  <View
                    style={[
                      styles.termCheck,
                      {
                        backgroundColor: on ? T2.brand : 'transparent',
                        borderWidth: on ? 0 : 1.5,
                        borderColor: T2.borderStrong,
                      },
                    ]}
                  >
                    {on ? <Text style={styles.termMark}>✓</Text> : null}
                  </View>
                  <Text style={styles.termLabel}>
                    <Text style={[styles.termTag, { color: t.req ? T2.text : T2.textMute }]}>
                      {t.req ? '필수 ' : '선택 '}
                    </Text>
                    {t.label}
                  </Text>
                  <Icon name="chevronRight" size={12} color={T2.textMute} />
                </Pressable>
              );
            })}
          </View>
        </ScrollView>

        {/* CTA */}
        <View style={styles.ctaWrap}>
          <CTAButton
            label="인증번호 받기"
            disabled={!canProceed}
            onPress={() => navigation.navigate('VerifyCode', { phone })}
          />
        </View>
      </KeyboardAvoidingView>
    </Screen>
  );
}

const styles = StyleSheet.create({
  header: { height: 56, justifyContent: 'center', paddingHorizontal: 20 },
  backArrow: { fontSize: 22, color: T2.text },
  scroll: { paddingHorizontal: 28, paddingBottom: 24 },

  h1: { fontSize: 30, fontWeight: '800', color: T2.text, letterSpacing: -1, lineHeight: 35 },
  lead: { fontSize: 14, color: T2.textSub, marginTop: 12, lineHeight: 21, letterSpacing: -0.3 },

  fieldLabel: { fontSize: 12, fontWeight: '700', color: T2.textMute, letterSpacing: 0.5, marginBottom: 10 },
  inputRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    paddingBottom: 12,
    borderBottomWidth: 2,
    borderBottomColor: T2.text,
  },
  cc: { fontSize: 22, fontWeight: '700', color: T2.textMute, letterSpacing: -0.5 },
  input: { flex: 1, fontSize: 22, fontWeight: '700', color: T2.text, letterSpacing: 0.5, padding: 0 },

  allRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    paddingBottom: 14,
    borderBottomWidth: 1,
    borderBottomColor: T2.border,
  },
  allCheck: {
    width: 20,
    height: 20,
    borderRadius: 10,
    borderWidth: 1.5,
    alignItems: 'center',
    justifyContent: 'center',
  },
  checkMark: { color: '#fff', fontSize: 11, fontWeight: '800' },
  allText: { flex: 1, fontSize: 14, fontWeight: '700', color: T2.text, letterSpacing: -0.3 },

  termRow: { flexDirection: 'row', alignItems: 'center', gap: 10, paddingVertical: 11 },
  termCheck: { width: 16, height: 16, borderRadius: 8, alignItems: 'center', justifyContent: 'center' },
  termMark: { color: '#fff', fontSize: 9, fontWeight: '800' },
  termLabel: { flex: 1, fontSize: 13, color: T2.textSub, letterSpacing: -0.2 },
  termTag: { fontWeight: '700' },

  ctaWrap: { paddingHorizontal: 24, paddingTop: 12, paddingBottom: 24 },
});
