// ProfileSetup — 프로필 완성 03/03 (원본: screens/ProfileSetup.jsx)
// 커스텀 폼 컨트롤을 Pressable 토글 + TextInput으로 구현. 음식은 최대 3개 다중선택.
import React, { useState, useEffect } from 'react';
import { View, Text, TextInput, Pressable, ScrollView, StyleSheet, Alert } from 'react-native';
import { Screen, StepProgress, CTAButton, FieldLabel, Icon } from '@/shared/components';
import { T2 } from '@/shared/theme';
import { apiGet, apiPost, ApiError } from '@/shared/api/client';
import { useAuth } from '@/shared/auth/AuthContext';
import type { RootStackScreenProps } from '@/navigation/types';

const FOODS = ['한식', '일식', '양식', '중식', '면 요리', '매운맛', '디저트'];
const STYLES_OPT = [
  { key: 'talk', label: '도란도란 대화하며', sub: '가볍게 이야기 나누는 게 좋아요' },
  { key: 'quiet', label: '조용히 각자', sub: '편하게, 말 없이 먹어도 좋아요' },
];

type Term = { key: string; label: string; req: boolean };
// 약관 키를 백엔드 /auth/terms 필드명과 동일하게 맞춘다(그대로 전송).
const TERMS: Term[] = [
  { key: 'service', label: '서비스 이용약관', req: true },
  { key: 'privacy', label: '개인정보 처리방침', req: true },
  { key: 'location', label: '위치정보 이용 동의', req: true },
  { key: 'marketing', label: '마케팅 알림 수신', req: false },
];

export function ProfileSetupScreen({ navigation, route }: RootStackScreenProps<'ProfileSetup'>) {
  const { onboardingToken } = route.params;
  const { signIn } = useAuth();
  const [submitting, setSubmitting] = useState(false);
  const [nickname, setNickname] = useState('혜린');
  const [intro, setIntro] = useState('조용히 먹는 것도, 도란도란 얘기하는 것도 좋아요.');
  const [gender, setGender] = useState<'female' | 'male'>('female');
  const [foods, setFoods] = useState<string[]>(['한식', '일식', '면 요리']);
  const [style, setStyle] = useState('talk');
  const [terms, setTerms] = useState<Record<string, boolean>>({
    service: false,
    privacy: false,
    location: false,
    marketing: false,
  });

  const allChecked = TERMS.every((t) => terms[t.key]);
  const requiredOk = TERMS.filter((t) => t.req).every((t) => terms[t.key]);
  const toggleAllTerms = () => setTerms(Object.fromEntries(TERMS.map((t) => [t.key, !allChecked])));
  const toggleTerm = (k: string) => setTerms((p) => ({ ...p, [k]: !p[k] }));

  // 닉네임 실시간 중복 확인. 입력이 멈추면(디바운스 400ms) nickname-check를 호출한다.
  // 2자 미만은 확인하지 않고(idle), 온보딩 토큰으로 호출한다(아직 정식 로그인 전).
  const [nickStatus, setNickStatus] = useState<'idle' | 'checking' | 'available' | 'taken' | 'error'>('idle');

  useEffect(() => {
    const trimmed = nickname.trim();
    if (trimmed.length < 2) {
      setNickStatus('idle');
      return;
    }
    setNickStatus('checking');
    const handle = setTimeout(async () => {
      try {
        const res = await apiGet<{ nickname: string; available: boolean }>(
          `/users/nickname-check?nickname=${encodeURIComponent(trimmed)}`,
          { token: onboardingToken },
        );
        setNickStatus(res.available ? 'available' : 'taken');
      } catch {
        setNickStatus('error');
      }
    }, 400);
    return () => clearTimeout(handle); // 다음 입력이 오면 이전 예약을 취소(디바운스)
  }, [nickname, onboardingToken]);

  const toggleFood = (f: string) => {
    setFoods((prev) => {
      if (prev.includes(f)) return prev.filter((x) => x !== f);
      if (prev.length >= 3) return prev;
      return [...prev, f];
    });
  };

  // 약관 제출 → 프로필 제출(가입 확정 + 정식 토큰) → 로그인(네비게이터가 메인으로 전환).
  // 좋아하는 음식은 백엔드 P1에 저장 필드가 없어 전송하지 않는다(UI 전용, P2 예정).
  const onComplete = async () => {
    if (!requiredOk) {
      Alert.alert('약관 동의 필요', '필수 약관에 모두 동의해주세요.');
      return;
    }
    setSubmitting(true);
    try {
      await apiPost(
        '/auth/terms',
        { service: terms.service, privacy: terms.privacy, location: terms.location, marketing: terms.marketing },
        { token: onboardingToken },
      );
      const tokens = await apiPost<{ accessToken: string; refreshToken: string }>(
        '/auth/complete',
        {
          nickname,
          gender: gender === 'female' ? 'FEMALE' : 'MALE',
          diningStyle: style === 'talk' ? 'TALK' : 'QUIET',
          introduction: intro,
          region: '마포구 연남동',
        },
        { token: onboardingToken },
      );
      await signIn(tokens);
    } catch (e) {
      Alert.alert('가입 완료 실패', e instanceof ApiError ? e.message : '잠시 후 다시 시도해주세요.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Screen bg={T2.bg}>
      {/* 헤더 */}
      <View style={styles.header}>
        <Pressable onPress={() => navigation.goBack()} hitSlop={10}>
          <Text style={styles.backArrow}>←</Text>
        </Pressable>
      </View>

      <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
        <StepProgress step={3} />

        <Text style={styles.h1}>프로필을{'\n'}완성해주세요</Text>
        <Text style={styles.lead}>
          <Text style={{ color: T2.text, fontWeight: '700' }}>같이 먹기</Text>를 신청하거나 받을 때, 상대에게 보여지는 정보예요.
        </Text>

        {/* 프로필 사진 */}
        <View style={styles.photoRow}>
          <View>
            <View style={styles.photo}>
              <Text style={styles.photoInitial}>혜</Text>
            </View>
            <View style={styles.cameraBadge}>
              <Text style={{ fontSize: 12 }}>📷</Text>
            </View>
          </View>
          <View style={{ flex: 1 }}>
            <Text style={styles.photoTitle}>프로필 사진 추가</Text>
            <Text style={styles.photoSub}>상대에게 보여지는 정보예요.{'\n'}얼굴 사진이면 더 좋아요.</Text>
          </View>
        </View>

        {/* 닉네임 */}
        <View style={{ marginTop: 32 }}>
          <FieldLabel>닉네임</FieldLabel>
          <View style={styles.nickRow}>
            <TextInput
              style={styles.nickInput}
              value={nickname}
              onChangeText={setNickname}
              maxLength={10}
              placeholder="닉네임"
              placeholderTextColor={T2.textMute}
            />
            {nickStatus !== 'idle' && (
              <Text
                style={[
                  styles.nickStatusText,
                  {
                    color:
                      nickStatus === 'available'
                        ? T2.brand
                        : nickStatus === 'taken'
                          ? '#E1493F'
                          : T2.textMute,
                  },
                ]}
              >
                {nickStatus === 'checking'
                  ? '확인 중…'
                  : nickStatus === 'available'
                    ? '사용 가능'
                    : nickStatus === 'taken'
                      ? '이미 사용 중'
                      : '확인 실패'}
              </Text>
            )}
          </View>
          <Text style={styles.hint}>한글·영문 2–10자 · 언제든 변경 가능</Text>
        </View>

        {/* 성별 · 연령대 */}
        <View style={styles.twoCol}>
          <View style={{ flex: 1 }}>
            <FieldLabel>성별</FieldLabel>
            <View style={{ flexDirection: 'row', gap: 6 }}>
              {[
                { k: 'female', l: '여성' },
                { k: 'male', l: '남성' },
              ].map((g) => {
                const on = gender === g.k;
                return (
                  <Pressable
                    key={g.k}
                    onPress={() => setGender(g.k as 'female' | 'male')}
                    style={[
                      styles.segment,
                      { backgroundColor: on ? T2.text : '#fff', borderColor: on ? T2.text : T2.border },
                    ]}
                  >
                    <Text style={{ fontSize: 14, fontWeight: '700', color: on ? '#fff' : T2.textMute }}>{g.l}</Text>
                  </Pressable>
                );
              })}
            </View>
          </View>
          <View style={{ flex: 1 }}>
            <FieldLabel>연령대</FieldLabel>
            <View style={styles.dropdown}>
              <Text style={{ fontSize: 14, fontWeight: '700', color: T2.text, letterSpacing: -0.3 }}>20대</Text>
              <Icon name="chevronDown" size={11} color={T2.textMute} />
            </View>
          </View>
        </View>

        {/* 한 줄 소개 */}
        <View style={{ marginTop: 28 }}>
          <FieldLabel>한 줄 소개</FieldLabel>
          <TextInput
            style={styles.introInput}
            value={intro}
            onChangeText={setIntro}
            multiline
            maxLength={60}
            placeholder="나를 한 줄로 소개해보세요"
            placeholderTextColor={T2.textMute}
          />
        </View>

        {/* 좋아하는 음식 */}
        <View style={{ marginTop: 28 }}>
          <FieldLabel>
            좋아하는 음식 <Text style={{ color: T2.textMute, fontWeight: '400' }}>· 최대 3개</Text>
          </FieldLabel>
          <View style={styles.chips}>
            {FOODS.map((f) => {
              const on = foods.includes(f);
              return (
                <Pressable
                  key={f}
                  onPress={() => toggleFood(f)}
                  style={[
                    styles.chip,
                    { backgroundColor: on ? T2.brand : '#fff', borderColor: on ? T2.brand : T2.border },
                  ]}
                >
                  <Text style={{ fontSize: 13, fontWeight: '600', color: on ? '#fff' : T2.text }}>{f}</Text>
                </Pressable>
              );
            })}
          </View>
        </View>

        {/* 같이 먹을 때 */}
        <View style={{ marginTop: 28 }}>
          <FieldLabel>같이 먹을 때</FieldLabel>
          {STYLES_OPT.map((r) => {
            const on = style === r.key;
            return (
              <Pressable
                key={r.key}
                onPress={() => setStyle(r.key)}
                style={[
                  styles.radioRow,
                  { backgroundColor: on ? T2.text : '#fff', borderColor: on ? T2.text : T2.border },
                ]}
              >
                <View style={[styles.radio, { borderColor: on ? T2.brand : T2.borderStrong, backgroundColor: on ? T2.brand : 'transparent' }]}>
                  {on ? <View style={styles.radioInner} /> : null}
                </View>
                <View style={{ flex: 1 }}>
                  <Text style={{ fontSize: 15, fontWeight: '700', color: on ? '#fff' : T2.text, letterSpacing: -0.3 }}>{r.label}</Text>
                  <Text style={{ fontSize: 12, color: on ? 'rgba(255,255,255,0.6)' : T2.textMute, marginTop: 1 }}>{r.sub}</Text>
                </View>
              </Pressable>
            );
          })}
        </View>

        {/* 우리 동네 */}
        <View style={{ marginTop: 28 }}>
          <FieldLabel>우리 동네</FieldLabel>
          <View style={styles.hood}>
            <View style={styles.hoodDot} />
            <View style={{ flex: 1 }}>
              <Text style={{ fontSize: 15, fontWeight: '700', color: T2.text, letterSpacing: -0.3 }}>마포구 연남동</Text>
              <Text style={{ fontSize: 11, color: T2.textMute, marginTop: 2 }}>현재 위치 기반 · 식당 142곳</Text>
            </View>
            <Text style={{ fontSize: 13, fontWeight: '700', color: T2.text }}>변경</Text>
          </View>
        </View>

        {/* 인증 배지 */}
        <View style={styles.verifyBadge}>
          <View style={styles.verifyIcon}>
            <Text style={{ color: '#fff', fontSize: 15, fontWeight: '800' }}>✓</Text>
          </View>
          <View style={{ flex: 1 }}>
            <Text style={{ fontSize: 13, fontWeight: '700', color: T2.text, letterSpacing: -0.3 }}>휴대폰 인증 완료</Text>
            <Text style={{ fontSize: 12, color: T2.textSub, marginTop: 1, letterSpacing: -0.2 }}>상대에게 '인증된 사용자'로 표시돼요</Text>
          </View>
        </View>

        {/* 약관 동의 — 신규 가입 시에만(이 화면은 신규 회원만 도달) */}
        <View style={styles.termsWrap}>
          <Pressable style={styles.allRow} onPress={toggleAllTerms}>
            <View style={[styles.allCheck, { backgroundColor: allChecked ? T2.text : '#fff', borderColor: allChecked ? T2.text : T2.borderStrong }]}>
              <Text style={styles.checkMark}>✓</Text>
            </View>
            <Text style={styles.allText}>약관에 모두 동의</Text>
          </Pressable>
          {TERMS.map((t) => {
            const on = terms[t.key];
            return (
              <Pressable key={t.key} style={styles.termRow} onPress={() => toggleTerm(t.key)}>
                <View
                  style={[
                    styles.termCheck,
                    { backgroundColor: on ? T2.brand : 'transparent', borderWidth: on ? 0 : 1.5, borderColor: T2.borderStrong },
                  ]}
                >
                  {on ? <Text style={styles.termMark}>✓</Text> : null}
                </View>
                <Text style={styles.termLabel}>
                  <Text style={[styles.termTag, { color: t.req ? T2.text : T2.textMute }]}>{t.req ? '필수 ' : '선택 '}</Text>
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
          label="시작하기"
          disabled={submitting || !requiredOk || !nickname.trim() || nickStatus === 'taken'}
          onPress={onComplete}
        />
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  header: { height: 56, justifyContent: 'center', paddingHorizontal: 20 },
  backArrow: { fontSize: 22, color: T2.text },
  scroll: { paddingHorizontal: 28, paddingBottom: 24 },

  h1: { fontSize: 30, fontWeight: '800', color: T2.text, letterSpacing: -1, lineHeight: 35 },
  lead: { fontSize: 14, color: T2.textSub, marginTop: 12, lineHeight: 21, letterSpacing: -0.3 },

  photoRow: { marginTop: 28, flexDirection: 'row', alignItems: 'center', gap: 16 },
  photo: { width: 72, height: 72, borderRadius: 36, backgroundColor: T2.text, alignItems: 'center', justifyContent: 'center' },
  photoInitial: { color: '#fff', fontSize: 28, fontWeight: '800' },
  cameraBadge: {
    position: 'absolute',
    right: -2,
    bottom: -2,
    width: 26,
    height: 26,
    borderRadius: 13,
    backgroundColor: T2.brand,
    borderWidth: 2.5,
    borderColor: T2.bg,
    alignItems: 'center',
    justifyContent: 'center',
  },
  photoTitle: { fontSize: 14, fontWeight: '700', color: T2.text, letterSpacing: -0.3 },
  photoSub: { fontSize: 12, color: T2.textMute, marginTop: 2, lineHeight: 17 },

  nickRow: { flexDirection: 'row', alignItems: 'center', gap: 10, paddingBottom: 12, borderBottomWidth: 2, borderBottomColor: T2.text },
  nickInput: { flex: 1, fontSize: 20, fontWeight: '700', color: T2.text, letterSpacing: -0.4, padding: 0 },
  nickStatusText: { fontSize: 12, fontWeight: '700' },
  hint: { marginTop: 8, fontSize: 11, color: T2.textMute },

  twoCol: { marginTop: 28, flexDirection: 'row', gap: 20 },
  segment: { flex: 1, paddingVertical: 11, borderRadius: 10, borderWidth: 1, alignItems: 'center' },
  dropdown: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 11,
    paddingHorizontal: 14,
    borderRadius: 10,
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: T2.border,
  },

  introInput: {
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
    minHeight: 60,
    textAlignVertical: 'top',
  },

  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  chip: { paddingVertical: 9, paddingHorizontal: 14, borderRadius: 999, borderWidth: 1 },

  radioRow: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 14, paddingHorizontal: 16, borderRadius: 12, marginBottom: 8, borderWidth: 1 },
  radio: { width: 18, height: 18, borderRadius: 9, borderWidth: 2, alignItems: 'center', justifyContent: 'center' },
  radioInner: { width: 6, height: 6, borderRadius: 3, backgroundColor: T2.text },

  hood: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 14, paddingHorizontal: 16, backgroundColor: '#fff', borderRadius: 12, borderWidth: 1, borderColor: T2.border },
  hoodDot: { width: 8, height: 8, borderRadius: 4, backgroundColor: T2.brand },

  verifyBadge: {
    marginTop: 16,
    paddingVertical: 14,
    paddingHorizontal: 16,
    borderRadius: 12,
    backgroundColor: 'rgba(255,90,31,0.06)',
    borderWidth: 1,
    borderColor: 'rgba(255,90,31,0.15)',
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  verifyIcon: { width: 32, height: 32, borderRadius: 16, backgroundColor: T2.brand, alignItems: 'center', justifyContent: 'center' },

  ctaWrap: { paddingHorizontal: 24, paddingTop: 12, paddingBottom: 24, backgroundColor: T2.bg },

  termsWrap: { marginTop: 28 },
  allRow: { flexDirection: 'row', alignItems: 'center', gap: 10, paddingBottom: 14, borderBottomWidth: 1, borderBottomColor: T2.border },
  allCheck: { width: 20, height: 20, borderRadius: 10, borderWidth: 1.5, alignItems: 'center', justifyContent: 'center' },
  checkMark: { color: '#fff', fontSize: 11, fontWeight: '800' },
  allText: { flex: 1, fontSize: 14, fontWeight: '700', color: T2.text, letterSpacing: -0.3 },
  termRow: { flexDirection: 'row', alignItems: 'center', gap: 10, paddingVertical: 11 },
  termCheck: { width: 16, height: 16, borderRadius: 8, alignItems: 'center', justifyContent: 'center' },
  termMark: { color: '#fff', fontSize: 9, fontWeight: '800' },
  termLabel: { flex: 1, fontSize: 13, color: T2.textSub, letterSpacing: -0.2 },
  termTag: { fontWeight: '700' },
});
