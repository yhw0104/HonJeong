// PhoneAuth — 휴대폰 번호 인증 01/03 (원본: screens/PhoneAuth.jsx)
// 전화번호를 입력받아 인증번호를 발송한다.
// (약관 동의는 신규 가입 단계인 ProfileSetup로 이동했다 — 기존 회원 로그인은 약관을 다시 볼 필요가 없기 때문.)
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
  Alert,
} from 'react-native';
import { Screen, StepProgress, CTAButton } from '@/shared/components';
import { T2 } from '@/shared/theme';
import { apiPost, ApiError } from '@/shared/api/client';
import type { RootStackScreenProps } from '@/navigation/types';

export function PhoneAuthScreen({ navigation }: RootStackScreenProps<'PhoneAuth'>) {
  const [phone, setPhone] = useState('');
  const [sending, setSending] = useState(false);

  const canProceed = phone.replace(/\D/g, '').length >= 8;

  // 인증번호 발송 → 성공 시 VerifyCode로(전화번호 전달).
  const onSendCode = async () => {
    const phoneDigits = phone.replace(/\D/g, '');
    setSending(true);
    try {
      await apiPost('/auth/phone/send-code', { phone: phoneDigits });
      navigation.navigate('VerifyCode', { phone: phoneDigits });
    } catch (e) {
      Alert.alert('인증번호 발송 실패', e instanceof ApiError ? e.message : '잠시 후 다시 시도해주세요.');
    } finally {
      setSending(false);
    }
  };

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
        </ScrollView>

        {/* CTA */}
        <View style={styles.ctaWrap}>
          <CTAButton label="인증번호 받기" disabled={!canProceed || sending} onPress={onSendCode} />
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

  ctaWrap: { paddingHorizontal: 24, paddingTop: 12, paddingBottom: 24 },
});
