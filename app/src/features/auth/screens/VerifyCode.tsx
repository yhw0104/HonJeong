// VerifyCode — 인증번호 입력 02/03 (원본: screens/VerifyCode.jsx)
// 6칸 박스 + 숨은 TextInput으로 실제 입력 캡처, 재전송 카운트다운 타이머.
import React, { useEffect, useRef, useState } from 'react';
import { View, Text, TextInput, Pressable, StyleSheet, Alert } from 'react-native';
import { Screen, StepProgress, CTAButton, Icon } from '@/shared/components';
import { T2 } from '@/shared/theme';
import { apiPost, ApiError } from '@/shared/api/client';
import { useAuth } from '@/shared/auth/AuthContext';
import { verifyGateState } from '@/features/auth/verifyGate';
import type { RootStackScreenProps } from '@/navigation/types';

const CELLS = 6;
const pad = (n: number) => String(n).padStart(2, '0');

export function VerifyCodeScreen({ navigation, route }: RootStackScreenProps<'VerifyCode'>) {
  const { phone } = route.params;
  const { signIn } = useAuth();
  const [code, setCode] = useState('');
  const [secs, setSecs] = useState(180);
  const [cooldown, setCooldown] = useState(30); // 진입도 방금 보낸 것이므로 30초 쿨다운으로 시작
  const [resending, setResending] = useState(false);
  const [verifying, setVerifying] = useState(false);
  const inputRef = useRef<TextInput>(null);

  useEffect(() => {
    const t = setInterval(() => {
      setSecs((s) => (s > 0 ? s - 1 : 0));
      setCooldown((c) => (c > 0 ? c - 1 : 0));
    }, 1000);
    return () => clearInterval(t);
  }, []);

  const onChange = (t: string) => setCode(t.replace(/\D/g, '').slice(0, CELLS));
  // 재전송 — 실제로 인증번호를 다시 요청한다(phone_verifications에 새 행 생성). 성공 시 코드 초기화+타이머 리셋.
  const resend = async () => {
    if (resending || cooldown > 0) return; // 연타·쿨다운 중 중복 발송 차단
    setResending(true);
    try {
      await apiPost('/auth/phone/send-code', { phone });
      setCode('');
      setSecs(180);
      setCooldown(30);
      inputRef.current?.focus();
    } catch (e) {
      Alert.alert('재전송 실패', e instanceof ApiError ? e.message : '잠시 후 다시 시도해주세요.');
    } finally {
      setResending(false);
    }
  };

  const gate = verifyGateState({ secs, resending, cooldown, codeLen: code.length, cells: CELLS, verifying });

  // 인증번호 확인. 신규=온보딩토큰 받아 약관 제출→ProfileSetup / 기존=토큰 받아 바로 로그인.
  const onVerify = async () => {
    setVerifying(true);
    try {
      const result = await apiPost<{
        onboarding: boolean;
        onboardingToken?: string;
        accessToken?: string;
        refreshToken?: string;
      }>('/auth/phone/verify', { phone, code });

      if (result.onboarding && result.onboardingToken) {
        // 신규 회원 → 약관 동의 + 프로필 작성을 위해 ProfileSetup로(약관은 거기서 제출).
        navigation.navigate('ProfileSetup', { onboardingToken: result.onboardingToken });
      } else if (result.accessToken && result.refreshToken) {
        await signIn({ accessToken: result.accessToken, refreshToken: result.refreshToken });
      } else {
        Alert.alert('인증 오류', '예상치 못한 응답입니다. 다시 시도해주세요.');
      }
    } catch (e) {
      Alert.alert('인증 실패', e instanceof ApiError ? e.message : '잠시 후 다시 시도해주세요.');
    } finally {
      setVerifying(false);
    }
  };

  const phoneLabel = phone ? `+82 ${phone}` : '+82 10 2580 ····';

  return (
    <Screen bg={T2.bg}>
      {/* 헤더 */}
      <View style={styles.header}>
        <Pressable onPress={() => navigation.goBack()} hitSlop={10}>
          <Text style={styles.backArrow}>←</Text>
        </Pressable>
      </View>

      <View style={styles.body}>
        <StepProgress step={2} />

        <Text style={styles.h1}>인증번호를{'\n'}입력해주세요</Text>
        <Text style={styles.lead}>
          <Text style={{ color: T2.text, fontWeight: '700' }}>{phoneLabel}</Text> 로 보낸{'\n'}6자리 숫자를 입력해주세요.
        </Text>

        {/* 코드 6칸 — 박스를 누르면 숨은 입력에 포커스 */}
        <Pressable style={styles.cells} onPress={() => inputRef.current?.focus()}>
          {Array.from({ length: CELLS }).map((_, i) => {
            const ch = code[i] ?? '';
            const active = i === code.length;
            const done = ch !== '';
            return (
              <View
                key={i}
                style={[
                  styles.cell,
                  { borderColor: active ? T2.text : done ? T2.borderStrong : T2.border },
                ]}
              >
                {done ? <Text style={styles.cellText}>{ch}</Text> : active ? <View style={styles.cursor} /> : null}
              </View>
            );
          })}
          <TextInput
            ref={inputRef}
            value={code}
            onChangeText={onChange}
            keyboardType="number-pad"
            maxLength={CELLS}
            autoFocus
            editable={!gate.expired}
            style={styles.hiddenInput}
            caretHidden
          />
        </Pressable>

        {/* 타이머 + 재전송 */}
        <View style={styles.timerRow}>
          <Text style={styles.timer}>{`${pad(Math.floor(secs / 60))}:${pad(secs % 60)}`}</Text>
          <Text style={styles.timerHint}>안에 입력해주세요</Text>
          <Pressable onPress={resend} disabled={!gate.canResend} style={{ marginLeft: 'auto' }}>
            <Text style={[styles.resend, !gate.canResend && { color: T2.textMute }]}>{gate.resendLabel}</Text>
          </Pressable>
        </View>

        {/* 보조 안내 */}
        <View style={styles.notice}>
          <Icon name="info" size={17} color={T2.textMute} />
          <Text style={styles.noticeText}>문자가 오지 않나요? 스팸함을 확인하거나 재전송해보세요.</Text>
        </View>
        {gate.expired ? (
          <Text style={styles.expired}>인증 시간이 지났어요. 재전송해 주세요.</Text>
        ) : null}
      </View>

      {/* CTA */}
      <View style={styles.ctaWrap}>
        <CTAButton
          label="인증 완료"
          disabled={!gate.canVerify}
          onPress={onVerify}
        />
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  header: { height: 56, justifyContent: 'center', paddingHorizontal: 20 },
  backArrow: { fontSize: 22, color: T2.text },
  body: { flex: 1, paddingHorizontal: 28 },

  h1: { fontSize: 30, fontWeight: '800', color: T2.text, letterSpacing: -1, lineHeight: 35 },
  lead: { fontSize: 14, color: T2.textSub, marginTop: 12, lineHeight: 21, letterSpacing: -0.3 },

  cells: { flexDirection: 'row', gap: 9, marginTop: 40 },
  cell: {
    flex: 1,
    height: 62,
    borderRadius: 12,
    borderWidth: 1.5,
    backgroundColor: '#fff',
    alignItems: 'center',
    justifyContent: 'center',
  },
  cellText: { fontSize: 26, fontWeight: '800', color: T2.text },
  cursor: { width: 2, height: 24, backgroundColor: T2.brand },
  hiddenInput: { position: 'absolute', width: 1, height: 1, opacity: 0 },

  timerRow: { flexDirection: 'row', alignItems: 'center', gap: 10, marginTop: 22 },
  timer: { fontSize: 13, fontWeight: '700', color: T2.brand, letterSpacing: -0.2 },
  timerHint: { fontSize: 13, color: T2.textMute, letterSpacing: -0.2 },
  resend: { fontSize: 13, fontWeight: '700', color: T2.textSub, letterSpacing: -0.2, textDecorationLine: 'underline' },

  notice: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    marginTop: 28,
    paddingVertical: 13,
    paddingHorizontal: 15,
    borderRadius: 12,
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: T2.border,
  },
  noticeText: { flex: 1, fontSize: 12, color: T2.textSub, letterSpacing: -0.2, lineHeight: 17 },
  expired: { marginTop: 14, fontSize: 13, fontWeight: '700', color: T2.brand, letterSpacing: -0.2 },

  ctaWrap: { paddingHorizontal: 24, paddingTop: 12, paddingBottom: 24 },
});
