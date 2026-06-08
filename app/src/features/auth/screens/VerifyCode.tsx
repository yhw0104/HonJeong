// VerifyCode — 인증번호 입력 02/03 (원본: screens/VerifyCode.jsx)
// 6칸 박스 + 숨은 TextInput으로 실제 입력 캡처, 재전송 카운트다운 타이머.
import React, { useEffect, useRef, useState } from 'react';
import { View, Text, TextInput, Pressable, StyleSheet } from 'react-native';
import { Screen, StepProgress, CTAButton, Icon } from '@/shared/components';
import { T2 } from '@/shared/theme';
import type { RootStackScreenProps } from '@/navigation/types';

const CELLS = 6;
const pad = (n: number) => String(n).padStart(2, '0');

export function VerifyCodeScreen({ navigation, route }: RootStackScreenProps<'VerifyCode'>) {
  const phone = route.params?.phone;
  const [code, setCode] = useState('');
  const [secs, setSecs] = useState(180);
  const inputRef = useRef<TextInput>(null);

  useEffect(() => {
    const t = setInterval(() => setSecs((s) => (s > 0 ? s - 1 : 0)), 1000);
    return () => clearInterval(t);
  }, []);

  const onChange = (t: string) => setCode(t.replace(/\D/g, '').slice(0, CELLS));
  const resend = () => {
    setCode('');
    setSecs(180);
    inputRef.current?.focus();
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
            style={styles.hiddenInput}
            caretHidden
          />
        </Pressable>

        {/* 타이머 + 재전송 */}
        <View style={styles.timerRow}>
          <Text style={styles.timer}>{`${pad(Math.floor(secs / 60))}:${pad(secs % 60)}`}</Text>
          <Text style={styles.timerHint}>안에 입력해주세요</Text>
          <Pressable onPress={resend} style={{ marginLeft: 'auto' }}>
            <Text style={styles.resend}>재전송</Text>
          </Pressable>
        </View>

        {/* 보조 안내 */}
        <View style={styles.notice}>
          <Icon name="info" size={17} color={T2.textMute} />
          <Text style={styles.noticeText}>문자가 오지 않나요? 스팸함을 확인하거나 재전송해보세요.</Text>
        </View>
      </View>

      {/* CTA */}
      <View style={styles.ctaWrap}>
        <CTAButton
          label="인증 완료"
          disabled={code.length < CELLS}
          onPress={() => navigation.navigate('ProfileSetup')}
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

  ctaWrap: { paddingHorizontal: 24, paddingTop: 12, paddingBottom: 24 },
});
